package org.lucentrix.metaframe.plugin.solr.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.plugin.solr.SolrConfig;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;

import java.io.IOException;

public class SolrConfigDeserializer extends JsonDeserializer<SolrConfig> {
    PasswordEncryptor encryptor;

    public SolrConfigDeserializer(ConfigEnv configEnv) {
        this.encryptor = configEnv.getEncryptor();
    }

    //TODO implement
    @Override
    public SolrConfig deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        return null;
    }
}
