package org.lucentrix.metaframe.crawler;

import org.apache.commons.lang3.StringUtils;
import org.lucentrix.metaframe.DocumentPage;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.runtime.DocumentConsumer;
import org.lucentrix.metaframe.runtime.DocumentRetriever;
import org.lucentrix.metaframe.util.FormatUtil;
import org.pf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrawlService implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(CrawlService.class);
    private final Logger statLogger = LoggerFactory.getLogger("Statistics");

    private final List<ShutdownEventListener> shutdownEventListeners;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();

    private final AtomicBoolean running = new AtomicBoolean();
    private final Lock shutdownLock = new ReentrantLock();
    private final Condition shutdownCompleted = shutdownLock.newCondition();
    private final ScheduledExecutorService statPrintExecutor;
    private final CrawlContext context;
    private final PluginManager pluginManager;

    private final DocumentRetriever source;
    private final DocumentConsumer target;

    public CrawlService(CrawlContext context) {
        this.context = context;
        this.pluginManager = new DefaultPluginManager() {
            @Override
            protected PluginFactory createPluginFactory() {
                return new CrawlPluginFactory(context);
            }
        };
        this.pluginManager.loadPlugins();

        // Load and start plugins
        //pluginManager.loadPlugins();
        // pluginManager.startPlugins();
        this.shutdownEventListeners = new ArrayList<>();

        CrawlConfig config = context.getConfig();

        long statisticsLogIntervalSec = config.getStatisticsIntervalSec();
        if (statisticsLogIntervalSec > 0 && statLogger.isInfoEnabled()) {
            this.statPrintExecutor = Executors.newScheduledThreadPool(1);
            this.statPrintExecutor.scheduleAtFixedRate(
                    () -> {
                        try {
                            CrawlStatistics statistics = context.getStatistics();
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

        String pluginId = StringUtils.trim(config.getSourcePluginId());

        if (StringUtils.isBlank(pluginId)) {
            throw new RuntimeException("Source plugin is is blank!");
        }

        logger.info("Loading source plugin id={}", pluginId);


        PluginWrapper pluginWrapper =  pluginManager.getPlugin(pluginId);
        if(pluginWrapper == null) {
            throw new RuntimeException("Plugin "+pluginId + " is not found");
        }
        pluginManager.startPlugin(pluginId);
        Plugin plugin = pluginWrapper.getPlugin();
        if (plugin instanceof DocumentRetriever) {
            this.source = (DocumentRetriever) plugin;
        } else {
            throw new RuntimeException("Source plugin id=\""+pluginId+"\" is not instance of "+DocumentRetriever.class+"!");
        }

        pluginId = StringUtils.trim(config.getTargetPluginId());

        if (StringUtils.isBlank(pluginId)) {
            throw new RuntimeException("Source plugin is is blank!");
        }

        logger.info("Loading source plugin id={}", pluginId);

        pluginWrapper =  pluginManager.getPlugin(pluginId);
        if(pluginWrapper == null) {
            throw new RuntimeException("Plugin "+pluginId + " is not found");
        }
        pluginManager.startPlugin(pluginId);
        plugin = pluginWrapper.getPlugin();
        if (plugin instanceof DocumentConsumer) {
            this.target = (DocumentConsumer) plugin;
        } else {
            throw new RuntimeException("Target plugin id=\""+pluginId+"\" is not instance of "+DocumentConsumer.class+"!");
        }

    }


    @Override
    public synchronized void run() {
        logger.info("Crawler started");

        running.set(true);

        try {
            boolean noMoreDocuments;

            do {
                try {
                    logger.debug("Next cycle started ");

                    noMoreDocuments = true;

                    if (source.hasNext()) {
                        DocumentPage page = source.next();
                        noMoreDocuments = page == null || !page.hasNext();

                        if (page != null && !page.isEmpty()) {
                            List<LxEvent> events = page.getItems();

                            target.push(events);

                            //Save crawl point
                            source.save();

                            //TODO refactor
                            context.getStatistics().getDocCounter().increment(events.size());

                            logger.info("Generated {} events, speed: {} event/hr, Run time: {}",
                                    context.getStatistics().getDocCounter().count(),
                                    context.getStatistics().getDocPerHour(),
                                    FormatUtil.toHumanReadable(context.getStatistics().getRunTimeMsec()));
                        }
                    }

                    if (isShutdownRequested()) {
                        break;
                    }

                    long sleepInterval = noMoreDocuments
                            ? context.getConfig().getIdleSleepIntervalMsec()
                            : context.getConfig().getSleepIntervalMsec();

                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepInterval);
                    } catch (InterruptedException e) {
                        logger.warn("Sleep interrupted, stopping crawler");
                        notifyShutdownListeners();
                    }
                } catch (Exception ex) {
                    logger.error("Error in crawl service ", ex);
                    notifyShutdownListeners();
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
        try {
            pluginManager.stopPlugins();
        } catch (Exception ex) {
            logger.warn("Error stopping plugins", ex);
        }

        if (isShutdownRequested()) {
            return;
        }

        this.shutdownRequested.set(true);

        logger.info("Shutdown - notifying crawl service shutdown event listeners");

        notifyShutdownListeners();

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

    private void notifyShutdownListeners() {
        for (ShutdownEventListener eventListener : shutdownEventListeners) {
            try {
                eventListener.shutdown();
            } catch (Exception ex) {
                logger.error("Error notifying shutdown event listener: " + eventListener, ex);
            }
        }
    }
}
