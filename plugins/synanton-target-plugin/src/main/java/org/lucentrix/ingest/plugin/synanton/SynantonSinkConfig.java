package org.lucentrix.ingest.plugin.synanton;

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
public class SynantonSinkConfig extends JsonPluginConfig<SynantonSinkConfig.Settings> {

    public SynantonSinkConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    public SynantonSinkConfig(Settings settings, ConfigEnv configEnv) {
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
                .name("synanton")
                .baseUrl("http://localhost:8088")
                .synfluxUrl("http://localhost:8090")
                .tenant("demo")
                .apiKey("")
                .build();
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @Getter
    public static class Settings {
        private String name;
        private String baseUrl;
        private String synfluxUrl;
        private String tenant;
        private String apiKey;
    }
}
