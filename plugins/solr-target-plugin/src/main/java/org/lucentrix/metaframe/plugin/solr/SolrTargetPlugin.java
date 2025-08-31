package org.lucentrix.metaframe.plugin.solr;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.runtime.DocumentConsumer;
import org.lucentrix.metaframe.runtime.plugin.AbstractPlugin;
import org.lucentrix.metaframe.runtime.plugin.ConsumerPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@ToString
@EqualsAndHashCode(callSuper = true)
public class SolrTargetPlugin extends AbstractPlugin<SolrTargetConfig, ConsumerPluginContext> implements DocumentConsumer {
    private final Logger logger = LoggerFactory.getLogger(SolrTargetPlugin.class);

    private final SolrIndexClient solrClient;

    private ScheduledExecutorService autoCommitExec;
    private final ReentrantLock indexLock = new ReentrantLock();
    private final AtomicInteger uncommittedEventCount = new AtomicInteger(0);
    private final AtomicLong lastCommitTime = new AtomicLong(System.currentTimeMillis());
    private final SolrConfig solrConfig;

    public SolrTargetPlugin(SolrTargetConfig config, ConsumerPluginContext context) {
        super(config, context);

        this.solrConfig = config.getSolrConfig();
        this.solrClient = new SolrIndexClient(this.solrConfig, new SolrFieldMapper(config.getSettings().getFieldMappings()));
    }

    private AutoCommitPolicy getCommitPolicy() {
        return this.solrConfig.getAutoCommitPolicy();
    }

    private boolean timeToCommit() {
        AutoCommitPolicy policy = getCommitPolicy();

        return policy.isEnabled()
                && (policy.getCommitDocCount() < 1 && policy.getCommitIntervalMs() < 1L)
                || (uncommittedEventCount.get() > policy.getCommitDocCount()
                && System.currentTimeMillis() - lastCommitTime.get() > policy.getCommitIntervalMs());
    }

    private boolean isScheduledAutoCommit() {
        AutoCommitPolicy policy = getCommitPolicy();
        return policy.isEnabled() && (policy.getCommitIntervalMs() > 0 || policy.getCommitDocCount() > 0);
    }

    @Override
    public void start() {
        super.start();
        this.solrClient.open();

        if (isScheduledAutoCommit()) {
            this.autoCommitExec = Executors.newScheduledThreadPool(1);
            this.autoCommitExec.scheduleAtFixedRate(
                    () -> {
                        try {
                            if (timeToCommit()) {
                                logger.info("Committing solr index");
                                solrClient.commit(config.getSettings().getSolrCommitOptions());
                            }
                        } catch (Exception ex) {
                            logger.warn("Solr index commit error", ex);
                        }
                    }, 0, Math.max(2000L, getCommitPolicy().getCommitIntervalMs()), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (autoCommitExec != null) {
            try {
                autoCommitExec.shutdown();
            } finally {
                autoCommitExec = null;
            }
        }
        try {
            this.solrClient.close();
        } catch (IOException ex) {
            logger.warn("Error closing SOLR client", ex);
        }
    }

    @Override
    public void push(List<LxEvent> documents) {
        indexLock.lock();
        try {
            solrClient.updateIndex(documents, config.getSettings().getSolrCommitOptions());

            uncommittedEventCount.addAndGet(documents.size());

            if (timeToCommit()) {
                try {
                    solrClient.commit(config.getSettings().getSolrCommitOptions());
                } finally {
                    lastCommitTime.set(System.currentTimeMillis());
                }
            }
        } finally {
            indexLock.unlock();
        }
    }
}
