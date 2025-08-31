package org.lucentrix.metaframe.serde.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.lucentrix.metaframe.metadata.StringObjectMap;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.metadata.mapping.SuffixSerde;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class StringObjectMapSerializer extends JsonSerializer<StringObjectMap> {

    @Override
    public void serialize(StringObjectMap objectMap, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                gen.writeNullField(field);
                continue;
            }

            TypeId typeId = SuffixSerde.SUFFIX_MAPPING.resolveTypeId(field);
            switch (typeId) {
                case DATETIME -> gen.writeStringField(field, String.valueOf(entry.getValue()));
                case DATETIME_LIST -> {
                    if (value instanceof Collection<?> dates) {
                        gen.writeStartArray();
                        for (Object date : dates) {
                            if (date == null) {
                                gen.writeNullField(field);
                            } else {
                                gen.writeStringField(field, String.valueOf(date));
                            }
                        }
                        gen.writeEndArray();
                    } else {
                        throw new RuntimeException("Unsupported dates value: " + value);
                    }
                }
                default -> gen.writeObjectField(entry.getKey(), entry.getValue());
            }
        }
        gen.writeEndObject();
    }
}
