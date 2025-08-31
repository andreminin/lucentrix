package org.lucentrix.metaframe.runtime.plugin;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Getter
public class RetrieverPluginContext extends PluginContext {
    private int pageSize;
}
