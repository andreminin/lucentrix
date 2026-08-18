package org.lucentrix.ingest.serde.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.lucentrix.ingest.metadata.FieldObjectMap;
import org.lucentrix.ingest.metadata.StringObjectMap;
import org.lucentrix.ingest.metadata.converter.SuffixObjectMapper;

import java.io.IOException;

public class FieldObjectMapDeserializer extends JsonDeserializer<FieldObjectMap> {
    StringObjectMapDeserializer stringObjectMapDeserializer = new StringObjectMapDeserializer();
    SuffixObjectMapper suffixObjectMapper = new SuffixObjectMapper();

    @Override
    public FieldObjectMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        StringObjectMap stringValueMap = stringObjectMapDeserializer.deserialize(p, ctxt);
        FieldObjectMap objectMap = suffixObjectMapper.convert(stringValueMap);

        return objectMap;
    }
}

