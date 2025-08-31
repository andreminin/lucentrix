package org.lucentrix.metaframe.metadata;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.value.FieldValue;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder(toBuilder = true)
public class StringValueMap implements Iterable<Map.Entry<String, FieldValue<?>>>, Serializable {
    private final Map<String, FieldValue<?>> fields;

    public FieldValue<?> get(String key) {
        return fields.get(key);
    }

    public Map<String, FieldValue<?>> getAll() {
        return fields;
    }

    @Override
    public Iterator<Map.Entry<String, FieldValue<?>>> iterator() {
        return fields.entrySet().iterator();
    }


    public static class StringValueMapBuilder {
        Map<String, FieldValue<?>> fields = new LinkedHashMap<>();

        public StringValueMapBuilder set(String key, FieldValue<?> value) {
            fields.put(key, value);
            return this;
        }
    }
}