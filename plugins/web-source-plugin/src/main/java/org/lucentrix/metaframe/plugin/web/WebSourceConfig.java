package org.lucentrix.metaframe.plugin.web;

import lombok.*;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;
import org.lucentrix.metaframe.runtime.plugin.JsonPluginConfig;

import java.io.InputStream;
import java.util.Set;


@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class WebSourceConfig extends JsonPluginConfig<WebSourceConfig.Settings> {

    public WebSourceConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    @Override
    protected Class<Settings> getConfigBeanClass() {
        return Settings.class;
    }

    public WebSourceConfig(Settings settings, ConfigEnv configEnv) {
        super(settings, configEnv);
    }

    @Override
    protected String getName() {
        return settings.getName();
    }

    @Override
    protected Settings defaultSettings() {
        return Settings.builder()
                .name("WebSource")
                .maxDepth(1)
                .url("localhost")
                .build();
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @Getter
    @Setter
    public static class Settings {
        private String name;
        private int maxDepth;
        private String url;
    }
}


