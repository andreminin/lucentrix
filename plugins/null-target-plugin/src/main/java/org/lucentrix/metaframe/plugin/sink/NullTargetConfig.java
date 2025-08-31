package org.lucentrix.metaframe.plugin.solr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;
import org.lucentrix.metaframe.runtime.plugin.JsonPluginConfig;

import java.io.InputStream;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class NullTargetConfig extends JsonPluginConfig<NullTargetConfig.Settings> {

    public NullTargetConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    public NullTargetConfig(Settings settings, ConfigEnv configEnv) {
        super(settings, configEnv);
    }

    @Override
    protected String getName() {
        return settings.getName();
    }

    @Override
    protected Class<Settings> getConfigBeanClass() {
        return Settings.class;
    }

    @Override
    protected Settings defaultSettings() {
        return Settings.builder()
                .name("NullTarget")
                .commitDurationMs(0L)
                .build();
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @Getter
    public static class Settings {

        private String name;
        private long commitDurationMs;

    }
}
