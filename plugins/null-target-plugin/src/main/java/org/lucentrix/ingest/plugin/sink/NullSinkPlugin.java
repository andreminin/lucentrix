package org.lucentrix.ingest.plugin.sink;

import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.runtime.SinkPlugin;
import org.lucentrix.ingest.runtime.plugin.AbstractPlugin;
import org.lucentrix.ingest.runtime.plugin.SinkContext;

import java.util.List;

public class NullSinkPlugin  extends AbstractPlugin<org.lucentrix.ingest.plugin.sink.NullSinkConfig, SinkContext> implements SinkPlugin {
    private final long commitDurationMs;

    public NullSinkPlugin(org.lucentrix.ingest.plugin.sink.NullSinkConfig config, SinkContext context) {
        super(config, context);
        this.commitDurationMs = Math.abs(getConfig().getSettings().getCommitDurationMs());
    }

    @Override
    public void push(List<ContentChange> documents) {
        if(commitDurationMs > 0) {
            try {
                Thread.sleep(commitDurationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
