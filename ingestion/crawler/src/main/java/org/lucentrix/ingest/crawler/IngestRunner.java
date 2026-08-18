package org.lucentrix.ingest.crawler;

import org.apache.commons.lang3.StringUtils;
import org.lucentrix.ingest.ChangePage;
import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.runtime.SinkPlugin;
import org.lucentrix.ingest.runtime.SourcePlugin;
import org.lucentrix.ingest.util.FormatUtil;
import org.pf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IngestRunner implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(IngestRunner.class);
    private final Logger statLogger = LoggerFactory.getLogger("Statistics");

    // private final List<ShutdownEventListener> shutdownEventListeners;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();

    private final AtomicBoolean running = new AtomicBoolean();
    private final Lock shutdownLock = new ReentrantLock();
    private final Condition shutdownCompleted = shutdownLock.newCondition();
    private final ScheduledExecutorService statPrintExecutor;
    private final IngestContext context;
    private final PluginManager pluginManager;

    private final SourcePlugin source;
    private final SinkPlugin target;
    private volatile String lastError;
    private final AtomicLong errorCount = new AtomicLong();
    private final ConcurrentLinkedDeque<String> recentIds = new ConcurrentLinkedDeque<>();
    private volatile boolean exitWhenIdle;

    public IngestRunner(IngestContext context) {

        this.context = context;
        this.pluginManager = new DefaultPluginManager() {
            @Override
            protected PluginFactory createPluginFactory() {
                return new IngestPluginFactory(context);
            }
        };
        this.pluginManager.loadPlugins();

        // Load and start plugins
        //pluginManager.loadPlugins();
        // pluginManager.startPlugins();
        // this.shutdownEventListeners = new ArrayList<>();

        IngestConfig config = context.getConfig();

        if (config.getSleepIntervalMsec() < 0 || config.getIdleSleepIntervalMsec() < 0) {
            throw new IllegalArgumentException("Sleep intervals must be non-negative");
        }

        long statisticsLogIntervalSec = config.getStatisticsIntervalSec();
        if (statisticsLogIntervalSec > 0 && statLogger.isInfoEnabled()) {
            this.statPrintExecutor = Executors.newScheduledThreadPool(1);
            this.statPrintExecutor.scheduleAtFixedRate(
                    () -> {
                        if (!running.get()) return;
                        try {
                            IngestStats statistics = context.getStatistics();
                            long upTime = statistics.getRunTimeMsec();

                            statLogger.info("Crawl statistics." +
                                            " Run time: {}." +
                                            " Documents: {}." +
                                            " Crawl speed: {} doc/hr.",
                                    FormatUtil.toHumanReadable(upTime),
                                    statistics.getDocCounter(), statistics.getDocPerHour());
                        } catch (Exception ex) {
                            logger.error("Error printing statistics", ex);
                        }
                    }, 0, statisticsLogIntervalSec, TimeUnit.SECONDS);
        } else {
            this.statPrintExecutor = null;
        }

        try {
            String pluginId = StringUtils.trim(config.getSourcePluginId());

            this.source = loadPlugin(pluginId, SourcePlugin.class);

            pluginId = StringUtils.trim(config.getTargetPluginId());

            this.target = loadPlugin(pluginId, SinkPlugin.class);
        } catch (Exception e) {
            if (statPrintExecutor != null) {
                statPrintExecutor.shutdownNow();
            }
            throw e;
        }
    }

    public void setExitWhenIdle(boolean exitWhenIdle) {
        this.exitWhenIdle = exitWhenIdle;
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getLastError() {
        return lastError;
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public List<String> getRecentIds() {
        return List.copyOf(recentIds);
    }

    private <T> T loadPlugin(String pluginId, Class<T> type) {
        String typeName = type.getName();

        if (StringUtils.isBlank(pluginId)) {
            throw new RuntimeException(typeName + " plugin id is blank!");
        }
        PluginWrapper wrapper = pluginManager.getPlugin(pluginId);
        if (wrapper == null) {
            throw new RuntimeException("Plugin " + pluginId + " not found");
        }
        pluginManager.startPlugin(pluginId);
        Plugin plugin = wrapper.getPlugin();
        if (type.isInstance(plugin)) {
            return type.cast(plugin);
        } else {
            throw new RuntimeException(typeName + " plugin is not instance of " + type.getName());
        }
    }


    @Override
    public void run() {
        logger.info("Crawler started");

        running.set(true);

        try {
            boolean noMoreDocuments;

            do {
                try {
                    logger.debug("Next cycle started ");

                    noMoreDocuments = true;

                    if (source.hasNext()) {
                        ChangePage page = source.next();
                        noMoreDocuments = (page == null) || !page.hasNext();
                        if (page == null || page.isEmpty()) {
                            continue;
                        }

                        List<ContentChange> events = page.getItems();

                        target.push(events);

                        source.save();

                        context.getStatistics().getDocCounter().increment(events.size());
                        for (ContentChange change : events) {
                            if (change != null && change.getId() != null) {
                                recentIds.addFirst(change.getId());
                                while (recentIds.size() > 20) {
                                    recentIds.removeLast();
                                }
                            }
                        }

                        logger.info("Generated {} events, speed: {} event/hr, Run time: {}",
                                context.getStatistics().getDocCounter().count(),
                                context.getStatistics().getDocPerHour(),
                                FormatUtil.toHumanReadable(context.getStatistics().getRunTimeMsec()));
                    }

                    if (isShutdownRequested()) {
                        break;
                    }

                    if (noMoreDocuments && exitWhenIdle) {
                        logger.info("Source is idle; exiting");
                        break;
                    }

                    long sleepInterval = noMoreDocuments
                            ? context.getConfig().getIdleSleepIntervalMsec()
                            : context.getConfig().getSleepIntervalMsec();

                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepInterval);
                    } catch (InterruptedException e) {
                        logger.warn("Sleep interrupted, stopping crawler");
                        Thread.currentThread().interrupt();
                        //notifyShutdownListeners();
                    }
                } catch (Exception ex) {
                    lastError = ex.getMessage();
                    errorCount.incrementAndGet();
                    logger.error("Error in ingest runner", ex);
                }
            } while (!isShutdownRequested());

            if (statPrintExecutor != null) {
                logger.warn("Shutdown scheduled statistics process");
                statPrintExecutor.shutdownNow();
            }
        } finally {
            running.set(false);

            logger.warn("Notifying crawler shutdown initiator");
            shutdownLock.lock();
            try {
                shutdownCompleted.signalAll();
            } finally {
                shutdownLock.unlock();
            }

            logger.warn("crawl service stopped");
        }
    }

    private boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

    public void shutdown() {
        if (!shutdownRequested.compareAndSet(false, true)) {
            return;
        }

        try {
            pluginManager.stopPlugins();
        } catch (Exception ex) {
            logger.warn("Error stopping plugins", ex);
        }

        logger.info("Shutdown - notifying crawl service shutdown event listeners");

        //  notifyShutdownListeners();

        if (running.get()) {
            int timeout = context.getConfig().getShutdownTimeoutSec() * 2;
            logger.info("Waiting {} seconds for crawl service to complete", timeout);

            shutdownLock.lock();
            try {
                if (!shutdownCompleted.await(timeout, TimeUnit.SECONDS)) {
                    logger.info("Exit crawl service waiting by timeout");
                }
            } catch (InterruptedException e) {
                logger.info("Crawl service shutdown waiting interrupted.");
            } finally {
                shutdownLock.unlock();
            }
        }
    }

    // private void notifyShutdownListeners() {
    //    for (ShutdownEventListener eventListener : shutdownEventListeners) {
    //        try {
    //            eventListener.shutdown();
    //        } catch (Exception ex) {
    //            logger.error("Error notifying shutdown event listener: " + eventListener, ex);
    //        }
    //      }
    //  }
}
