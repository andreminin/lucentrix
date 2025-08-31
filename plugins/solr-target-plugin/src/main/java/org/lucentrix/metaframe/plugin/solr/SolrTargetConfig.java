package org.lucentrix.metaframe.plugin.solr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.metaframe.plugin.solr.json.SolrConfigDeserializer;
import org.lucentrix.metaframe.plugin.solr.json.SolrConfigSerializer;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;
import org.lucentrix.metaframe.runtime.plugin.JsonPluginConfig;

import java.io.InputStream;
import java.util.List;
import java.util.Set;


@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class SolrTargetConfig extends JsonPluginConfig<SolrTargetConfig.Settings> {

    public SolrTargetConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    public SolrTargetConfig(Settings settings, ConfigEnv configEnv) {
        super(settings, configEnv);
    }

    @Override
    protected Class<Settings> getConfigBeanClass() {
        return Settings.class;
    }

    @Override
    protected String getName() {
        return settings.getName();
    }

    @Override
    protected Settings defaultSettings() {
        return Settings.builder()
                .name("SolrTarget")
                .solrUrls(List.of("http://localhost:8983/solr/dummy"))
                .solrCommitOptions(SolrCommitOptions.builder().build())
                .build();
    }

    protected ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(SolrConfig.class, new SolrConfigSerializer(configEnv));
        module.addDeserializer(SolrConfig.class, new SolrConfigDeserializer(configEnv));

        mapper.registerModule(module);

        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    public SolrConfig getSolrConfig() {

        return SolrConfig.builder()
                .solrUrls(settings.getSolrUrls())
                .zkHosts(settings.getZkHosts())
                .zkChroot(settings.getZkChroot())
                .collection(settings.getCollection())
                .user(settings.getUser())
                .passwordSupplier(getConfigEnv().getEncryptor() == null ?
                        () -> settings.getPassword() : () -> getConfigEnv().getEncryptor().decrypt(settings.getPassword()))
                .autoCommitPolicy(settings.getAutoCommitPolicy() == null ? new AutoCommitPolicy() : settings.getAutoCommitPolicy())
                .solrCommitOptions(settings.getSolrCommitOptions() == null ? new SolrCommitOptions() : settings.getSolrCommitOptions())
        .build();
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @Getter
    public static class Settings {

        private String name;
        private List<String> solrUrls;
        private List<String> zkHosts;
        private String zkChroot;
        private String collection;
        private String user;
        private String password;
        private AutoCommitPolicy autoCommitPolicy;
        private SolrCommitOptions solrCommitOptions;
        private Set<SolrFieldMapping> fieldMappings;
    }
}
