package org.lucentrix.metaframe.plugin.solr.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.plugin.solr.SolrConfig;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;

import java.io.IOException;

public class SolrConfigSerializer extends JsonSerializer<SolrConfig> {
    PasswordEncryptor encryptor;

    public SolrConfigSerializer(ConfigEnv configEnv) {
        this.encryptor = configEnv.getEncryptor();
    }

    //TODO implement
    @Override
    public void serialize(SolrConfig solrConfig, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

    }
}
