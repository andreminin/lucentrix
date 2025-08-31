package org.lucentrix.metaframe.plugin.sink;

import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.runtime.DocumentConsumer;
import org.lucentrix.metaframe.runtime.plugin.AbstractPlugin;
import org.lucentrix.metaframe.runtime.plugin.ConsumerPluginContext;

import java.util.List;

public class NullTargetPlugin  extends AbstractPlugin<org.lucentrix.metaframe.plugin.solr.NullTargetConfig, ConsumerPluginContext> implements DocumentConsumer {
    private final long commitDurationMs;

    public NullTargetPlugin(org.lucentrix.metaframe.plugin.solr.NullTargetConfig config, ConsumerPluginContext context) {
        super(config, context);
        this.commitDurationMs = Math.abs(getConfig().getSettings().getCommitDurationMs());
    }

    @Override
    public void push(List<LxEvent> documents) {
        if(commitDurationMs > 0) {
            try {
                Thread.sleep(commitDurationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
