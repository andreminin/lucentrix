package org.lucentrix.metaframe.plugin.dummy;

import lombok.*;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;
import org.lucentrix.metaframe.runtime.plugin.JsonPluginConfig;

import java.io.InputStream;


@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class DummySourceConfig extends JsonPluginConfig<DummySourceConfig.Settings> {

    public DummySourceConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    @Override
    protected Class<Settings> getConfigBeanClass() {
        return Settings.class;
    }

    public DummySourceConfig(Settings settings, ConfigEnv configEnv) {
        super(settings, configEnv);
    }

    @Override
    protected String getName() {
        return settings.getName();
    }

    @Override
    protected Settings defaultSettings() {
        return Settings.builder()
                .name("DummySource")
                .claimMaxCount(10 * 1000 * 1000)
                .clientMaxCount(5 * 1000 * 1000)
                .policyMaxCount(100 * 1000)
                .securityMaxCount(500)
                .userCount(1000)
                .groupCount(50)
                .build();
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @Getter
    @Setter
    public static class Settings {
        private String name;
        private int claimMaxCount;
        private int clientMaxCount;
        private int policyMaxCount;
        private int securityMaxCount;
        private int userCount;
        private int groupCount;


    }
}


