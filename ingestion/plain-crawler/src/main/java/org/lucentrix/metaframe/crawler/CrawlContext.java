package org.lucentrix.metaframe.crawler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.lucentrix.metaframe.ItemPersistence;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.runtime.persistence.FSJsonPersistence;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;
import org.lucentrix.metaframe.runtime.plugin.ConsumerPluginContext;
import org.lucentrix.metaframe.runtime.plugin.PluginContext;
import org.lucentrix.metaframe.runtime.plugin.RetrieverPluginContext;

import java.time.Instant;

@Getter
@EqualsAndHashCode
public class CrawlContext {
    private final Instant startTime;
    private final CrawlConfig config;
    private final ItemPersistence persistence;
    private final CrawlStatistics statistics;
    private final ConfigEnv configEnv;

    public CrawlContext(CrawlConfig config) {
        this.config = config;
        this.persistence = new FSJsonPersistence(config.getPersistencePath());
        this.statistics = new CrawlStatistics(config.getId(), new SimpleMeterRegistry());
        //TODO make encryptor configurable
        this.configEnv = ConfigEnv.builder().encryptor(new PasswordEncryptor()).build();
        this.startTime = Instant.now();
    }

    public RetrieverPluginContext createRetrieverPluginContext(String pluginId) {
        return RetrieverPluginContext.builder()
                .configSupplier(config.getPluginConfig(pluginId))
                .pageSize(config.getSourcePageSize())
                .persistence(persistence)
                .pluginId(pluginId)
                .build();
    }

    public ConsumerPluginContext createConsumerPluginContext(String pluginId) {
        return ConsumerPluginContext.builder()
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
