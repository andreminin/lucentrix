package org.lucentrix.metaframe.runtime.plugin;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.ItemPersistence;

import java.io.InputStream;
import java.time.Instant;
import java.util.function.Supplier;

@SuperBuilder
@Getter
@EqualsAndHashCode
public class PluginContext {
    private final Instant startTime = Instant.now();
    private String pluginId;
    private ItemPersistence persistence;
    private Supplier<InputStream> configSupplier;
}
