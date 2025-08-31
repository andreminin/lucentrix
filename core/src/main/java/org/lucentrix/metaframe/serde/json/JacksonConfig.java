package org.lucentrix.metaframe.serde.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.StringObjectMap;

public class JacksonConfig {

    public static ObjectMapper configureMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(StringObjectMap.class, new StringObjectMapSerializer());
        module.addDeserializer(StringObjectMap.class, new StringObjectMapDeserializer());

        module.addSerializer(FieldObjectMap.class, new FieldObjectMapSerializer());
        module.addDeserializer(FieldObjectMap.class, new FieldObjectMapDeserializer());

        mapper.registerModule(module);

        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
