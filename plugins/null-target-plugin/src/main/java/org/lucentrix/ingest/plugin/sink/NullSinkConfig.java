package org.lucentrix.ingest.plugin.sink;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.ingest.runtime.plugin.ConfigEnv;
import org.lucentrix.ingest.runtime.plugin.JsonPluginConfig;

import java.io.InputStream;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class NullSinkConfig extends JsonPluginConfig<NullSinkConfig.Settings> {

    public NullSinkConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    public NullSinkConfig(Settings settings, ConfigEnv configEnv) {
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
