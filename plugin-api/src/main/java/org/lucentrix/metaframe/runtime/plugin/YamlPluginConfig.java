package org.lucentrix.metaframe.runtime.plugin;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public abstract class YamlPluginConfig<S> extends PluginConfig<S> {

    protected YamlPluginConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    @Override
    protected S load(InputStream is) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(is);
        return apply(map);
    }

    protected abstract S apply(Map<String, Object> map);
}