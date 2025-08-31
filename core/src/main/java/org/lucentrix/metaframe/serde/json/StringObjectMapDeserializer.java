package org.lucentrix.metaframe.serde.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.lucentrix.metaframe.metadata.StringObjectMap;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.metadata.mapping.SuffixSerde;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class StringObjectMapDeserializer extends JsonDeserializer<StringObjectMap> {

    @Override
    public StringObjectMap deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectNode node = parser.getCodec().readTree(parser);
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = iter.next();
            fields.put(entry.getKey(), parseJsonNode(entry.getKey(), entry.getValue()));
        }

        return new StringObjectMap(fields);
    }

    private Object parseJsonNode(String field, com.fasterxml.jackson.databind.JsonNode node) {
        if(node.isNull()) {
            return null;
        }

        TypeId typeId = SuffixSerde.SUFFIX_MAPPING.resolveTypeId(field);

        switch (typeId) {
            case DATETIME ->  {
                return getInstant(node);
            }
            case DATETIME_LIST -> {
                if(node.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) node;
                    List<Instant> dates = new ArrayList<>();
                    for(Iterator<JsonNode> it = arrayNode.elements(); it.hasNext(); ) {
                        dates.add(getInstant(it.next()));
                    }
                    return dates;
                } else {
                    throw new RuntimeException("Unsupported date list json value: "+node);
                }
            }
        }
        if (node.isInt()) return node.intValue();
        if (node.isLong()) return node.longValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isDouble()) return node.doubleValue();
        if (node.isTextual()) return node.textValue();

        return node.toString();
    }

    private Instant getInstant(JsonNode node) {
        if(node.isTextual()) {
            return Instant.parse(node.textValue());
        }
        if(node.isLong()) {
            return Instant.ofEpochMilli(node.longValue());
        }
        throw new RuntimeException("Unsupported date json value: "+ node);
    }
}
