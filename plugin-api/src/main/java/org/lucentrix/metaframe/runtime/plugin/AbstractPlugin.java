package org.lucentrix.metaframe.runtime.plugin;

import org.lucentrix.metaframe.ItemPersistence;
import org.pf4j.Plugin;

public abstract class AbstractPlugin<N extends PluginConfig<?>, P extends PluginContext> extends Plugin {
    protected N config;
    protected P context;

    public AbstractPlugin(N config, P context) {
        if (config == null) {
            throw new IllegalArgumentException("Config is null!");
        }
        if (context == null) {
            throw new IllegalArgumentException("PluginContext is null!");
        }

        this.config = config;
        this.context = context;
    }

    public N getConfig() {
        return config;
    }

    protected ItemPersistence getPersistence() {
        return getContext().getPersistence();
    }

    public P getContext() {
        return context;
    }
}
