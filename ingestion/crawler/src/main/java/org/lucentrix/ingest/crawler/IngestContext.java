package org.lucentrix.ingest.crawler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.lucentrix.ingest.ItemPersistence;
import org.lucentrix.ingest.encrypt.PasswordEncryptor;
import org.lucentrix.ingest.runtime.persistence.FileJsonPersistence;
import org.lucentrix.ingest.runtime.plugin.ConfigEnv;
import org.lucentrix.ingest.runtime.plugin.SinkContext;
import org.lucentrix.ingest.runtime.plugin.PluginContext;
import org.lucentrix.ingest.runtime.plugin.SourceContext;

import java.time.Instant;

@Getter
@EqualsAndHashCode
public class IngestContext {
    private final Instant startTime;
    private final IngestConfig config;
    private final ItemPersistence persistence;
    private final IngestStats statistics;
    private final ConfigEnv configEnv;

    public IngestContext(IngestConfig config) {
        this.config = config;
        this.persistence = new FileJsonPersistence(config.getPersistencePath());
        this.statistics = new IngestStats(config.getId(), new SimpleMeterRegistry());
        PasswordEncryptor encryptor = config.getEncryptor() != null
                ? config.getEncryptor()
                : new PasswordEncryptor();
        this.configEnv = ConfigEnv.builder().encryptor(encryptor).build();
        this.startTime = Instant.now();
    }

    public SourceContext createSourceContext(String pluginId) {
        return SourceContext.builder()
                .configSupplier(config.getPluginConfig(pluginId))
                .pageSize(config.getSourcePageSize())
                .persistence(persistence)
                .pluginId(pluginId)
                .build();
    }

    public SinkContext createSinkContext(String pluginId) {
        return SinkContext.builder()
                .configSupplier(config.getPluginConfig(pluginId))
                .persistence(persistence)
                .pluginId(pluginId)
                .build();
    }

    public PluginContext createPlainPluginContext(String pluginId) {
        return PluginContext.builder()
                .configSupplier(config.getPluginConfig(pluginId))
                .persistence(persistence)
                .pluginId(pluginId)
                .build();
    }
}
