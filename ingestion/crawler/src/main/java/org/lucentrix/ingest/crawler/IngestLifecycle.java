package org.lucentrix.ingest.crawler;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class IngestLifecycle {

    private static final Logger log = LoggerFactory.getLogger(IngestLifecycle.class);

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lucentrix-ingest");
        t.setDaemon(true);
        return t;
    });

    private final AtomicReference<IngestRunner> runner = new AtomicReference<>();
    private volatile IngestContext context;

    public synchronized void start(boolean exitWhenIdle) {
        IngestRunner current = runner.get();
        if (current != null && current.isRunning()) {
            throw new IllegalStateException("Ingestion is already running");
        }
        Properties properties = loadProperties();
        IngestConfig config = new IngestConfig(properties);
        this.context = new IngestContext(config);
        IngestRunner next = new IngestRunner(context);
        next.setExitWhenIdle(exitWhenIdle);
        runner.set(next);
        Runtime.getRuntime().addShutdownHook(new Thread(next::shutdown, "lucentrix-shutdown"));
        worker.submit(next);
        log.info("Ingestion started source={} sink={}", config.getSourcePluginId(), config.getTargetPluginId());
    }

    public synchronized void stop() {
        IngestRunner current = runner.get();
        if (current != null) {
            current.shutdown();
        }
    }

    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        IngestConfig config = context == null ? null : context.getConfig();
        IngestRunner current = runner.get();
        IngestStats stats = context == null ? null : context.getStatistics();
        body.put("jobId", config == null ? null : config.getId());
        body.put("sourcePlugin", config == null ? null : config.getSourcePluginId());
        body.put("targetPlugin", config == null ? null : config.getTargetPluginId());
        body.put("running", current != null && current.isRunning());
        body.put("docs", stats == null ? 0 : stats.getDocCounter().count());
        body.put("docsPerHour", stats == null ? 0 : stats.getDocPerHour());
        body.put("uptimeMs", stats == null ? 0 : stats.getRunTimeMsec());
        body.put("errors", current == null ? 0 : current.getErrorCount());
        body.put("lastError", current == null ? null : current.getLastError());
        body.put("recentIds", current == null ? java.util.List.of() : current.getRecentIds());
        return body;
    }

    @PreDestroy
    public void destroy() {
        stop();
        worker.shutdownNow();
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        Path cwd = Path.of("application.properties");
        if (Files.isRegularFile(cwd)) {
            try (InputStream in = new FileInputStream(cwd.toFile())) {
                properties.load(in);
                return properties;
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load ./application.properties", ex);
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load classpath application.properties", ex);
        }
        return properties;
    }
}
