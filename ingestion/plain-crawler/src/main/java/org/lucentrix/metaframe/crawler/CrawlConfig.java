package org.lucentrix.metaframe.crawler;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.encrypt.SecretSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

@Data
@Getter
@ToString(exclude = "secretSettings")
@Builder
@AllArgsConstructor
public class CrawlConfig {
    private static final Logger logger = LoggerFactory.getLogger(CrawlConfig.class);

    private final String id;
    private final int statisticsIntervalSec;
    private final int shutdownTimeoutSec;
    private final long idleSleepIntervalMsec;
    private final long sleepIntervalMsec;
    private final String persistencePath;
    private final int sourcePageSize;
    private final PasswordEncryptor encryptor;
    private final SecretSettings secretSettings;

    private final String sourcePluginId;
    private final String targetPluginId;
    private final Map<String, String> pluginConfigPaths;


    public CrawlConfig() {
        this(new Properties());
    }

    public CrawlConfig(Properties properties) {
        Map<String, String> configMap = new LinkedHashMap<>();

        // Detect unique plugin groups (e.g., source, target, transformers.1, etc.)
        Set<String> pluginIds = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.endsWith(".plugin.id")) {
                String pluginId = properties.getProperty(key);
                if(StringUtils.isNotBlank(pluginId)) {
                    pluginIds.add(pluginId);
                }
            }
        }

        for (String pluginId : pluginIds) {
            String configKey = pluginId + ".config";
            String configPath = StringUtils.trim(properties.getProperty(configKey, null));

            if (StringUtils.isNotBlank(configPath)) {
                configMap.put(pluginId, configPath);
            } else {
                logger.warn("Warning: Missing config path for plugin id \"" + pluginId + "\"");
            }
        }

        this.secretSettings = SecretSettings.builder()
                .iv(properties.getProperty("encryption.iv"))
                .salt(properties.getProperty("encryption.salt"))
                .secret(properties.getProperty("encryption.secret"))
                .build();

        this.id = properties.getProperty("id", "crawler");
        this.encryptor = new PasswordEncryptor(secretSettings);
        this.statisticsIntervalSec = Integer.parseInt(properties.getProperty("statistics.interval.sec", "120"));
        this.shutdownTimeoutSec = Integer.parseInt(properties.getProperty("shutdown.timeout.sec", "30"));
        this.sleepIntervalMsec = Long.parseLong(properties.getProperty("sleep.interval.ms", "500"));
        this.idleSleepIntervalMsec = Long.parseLong(properties.getProperty("idle.sleep.interval.ms", "10000"));
        this.persistencePath = properties.getProperty("persistence.path", "storage");
        this.sourcePluginId = properties.getProperty("source.plugin.id");
        this.targetPluginId = properties.getProperty("target.plugin.id");
        this.sourcePageSize = Integer.parseInt(properties.getProperty("source.page.size", "1000"));
        this.pluginConfigPaths = configMap;

    }

    public void save(Properties properties) {
        properties.setProperty("id", this.id);

        properties.setProperty("encryption.secret", secretSettings.getSecret());
        properties.setProperty("encryption.iv", secretSettings.getIv());
        properties.setProperty("encryption.salt", secretSettings.getSalt());

        properties.setProperty("statistics.interval.sec", String.valueOf(this.getStatisticsIntervalSec()));
        properties.setProperty("shutdown.timeout.sec", String.valueOf(this.getShutdownTimeoutSec()));
        properties.setProperty("sleep.interval.ms", String.valueOf(this.getSleepIntervalMsec()));
        properties.setProperty("idle.sleep.interval.ms", String.valueOf(this.getIdleSleepIntervalMsec()));
        properties.setProperty("persistence.path", this.getPersistencePath());
        properties.setProperty("source.plugin.id", this.getSourcePluginId());
        properties.setProperty("target.plugin.id", this.getTargetPluginId());
        properties.setProperty("source.page.size", String.valueOf(this.getSourcePageSize()));

        for(Map.Entry<String,String> entry : this.getPluginConfigPaths().entrySet()) {
            properties.setProperty(entry.getKey() + ".config", entry.getValue());
        }

    }

    public Supplier<InputStream> getPluginConfig(String pluginId) {
        String uriString = StringUtils.trim(pluginConfigPaths.get(pluginId));

        if (StringUtils.isBlank(uriString)) {
            return () -> null;
        }

        if (uriString.startsWith("classpath:")) {
            String resourcePath = uriString.substring("classpath:".length());
            return () -> {
                InputStream is = getClass().getResourceAsStream(resourcePath);
                if (is == null) {
                    throw new UncheckedIOException(
                            new FileNotFoundException("Classpath resource not found: " + resourcePath));
                }
                return is;
            };
        } else if (uriString.startsWith("file:")) {
            Path path = Paths.get(URI.create(uriString));
            return () -> {
                try {
                    return Files.newInputStream(path);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read file: " + path, e);
                }
            };
        } else if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return () -> {
                try {
                    return new URL(uriString).openStream();
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to fetch remote config: " + uriString, e);
                }
            };
        } else {
            // Treat as relative file path
            Path path = Paths.get(uriString);
            return () -> {
                try {
                    return Files.newInputStream(path);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read local config file: " + path, e);
                }
            };
        }
    }
}
