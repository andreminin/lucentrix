package org.lucentrix.metaframe.metadata;

import lombok.*;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class StringObjectMap implements Iterable<Map.Entry<String,Object>>, Serializable {
    private LinkedHashMap<String, Object> fields;

    public Object get(String key) {
        return fields.get(key);
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return fields.entrySet().iterator();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return fields.entrySet();
    }

    public static class StringObjectMapBuilder {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        public StringObjectMapBuilder set(String key, Object value) {
            this.fields.put(key, value);
            return this;
        }
    }
}
