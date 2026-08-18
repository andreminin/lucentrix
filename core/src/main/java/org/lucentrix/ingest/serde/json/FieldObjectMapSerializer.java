package org.lucentrix.ingest.serde.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.lucentrix.ingest.metadata.FieldObjectMap;
import org.lucentrix.ingest.metadata.StringObjectMap;
import org.lucentrix.ingest.metadata.converter.SuffixObjectMapper;

import java.io.IOException;

public class FieldObjectMapSerializer extends JsonSerializer<FieldObjectMap> {
    SuffixObjectMapper suffixObjectMapper = new SuffixObjectMapper();

    @Override
    public void serialize(FieldObjectMap objectMap, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        StringObjectMap stringObjectMap = suffixObjectMapper.convert(objectMap);

        serializers.defaultSerializeValue(stringObjectMap, gen);
    }
}
