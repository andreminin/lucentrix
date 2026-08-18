package org.lucentrix.ingest.plugin.synanton;

import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.SourceDocument;
import org.lucentrix.ingest.runtime.SinkPlugin;
import org.lucentrix.ingest.runtime.plugin.AbstractPlugin;
import org.lucentrix.ingest.runtime.plugin.SinkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SynantonSinkPlugin extends AbstractPlugin<SynantonSinkConfig, SinkContext> implements SinkPlugin {

    private static final Logger log = LoggerFactory.getLogger(SynantonSinkPlugin.class);
    private final SynantonClient client;

    public SynantonSinkPlugin(SynantonSinkConfig config, SinkContext context) {
        super(config, context);
        SynantonSinkConfig.Settings settings = config.getSettings();
        this.client = new SynantonClient(
                settings.getBaseUrl(),
                settings.getSynfluxUrl(),
                settings.getTenant(),
                settings.getApiKey());
    }

    SynantonSinkPlugin(SynantonSinkConfig config, SinkContext context, SynantonClient client) {
        super(config, context);
        this.client = client;
    }

    @Override
    public void push(List<ContentChange> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<UUID> refs = new ArrayList<>();
        for (ContentChange change : documents) {
            if (change == null || change.getDocument() == null) {
                continue;
            }
            if (SynantonPayload.isDelete(change)) {
                log.warn("DELETE not implemented for Synanton v1; skipping id={}", change.getId());
                continue;
            }
            SourceDocument document = change.getDocument();
            UUID ref = client.push(
                    SynantonPayload.bytes(document),
                    SynantonPayload.sourceUri(document),
                    SynantonPayload.mimeType(document));
            refs.add(ref);
        }
        client.enqueue(refs);
    }
}
