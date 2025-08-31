package org.lucentrix.metaframe.metadata;


import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.metadata.field.Field;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;


@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public class FieldObjectMap implements Iterable<Map.Entry<Field<?>, Object>>, Serializable {

    private final Map<Field<?>, Object> fields;

    public FieldObjectMap() {
        this(new LinkedHashMap<>());
    }

    public FieldObjectMap(Map<Field<?>, Object> fields) {
        this.fields = fields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fields);
    }

    public <T> T get(Field<T> name) {
        return get(name, null);
    }

    public <T> T get(Field<T> name, T defaultValue) {
        T value = (T) fields.get(name);
        return value == null ? defaultValue : value;
    }

    public String getId() {
        return get(Field.ID);
    }

    public String getTitle() {
        return get(Field.TITLE);
    }

    public String getClassName() {
        return get(Field.CLASS_NAME);
    }

    public boolean isFolder() {
        return Boolean.TRUE.equals(get(Field.IS_FOLDER));
    }

    public Instant getModifyDatetime() {
        return get(Field.MODIFY_DATETIME);
    }

    public Instant getDateCreated() {
        return get(Field.CREATE_DATETIME);
    }

    public Set<Field<?>> keySet() {
        return new HashSet<>(fields.keySet());
    }

    @Override
    public Iterator<Map.Entry<Field<?>, Object>> iterator() {
        return fields.entrySet().iterator();
    }

    public boolean containsKey(Field<?> key) {
        return fields.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return fields.containsValue(value);
    }

    public Set<Map.Entry<Field<?>, Object>> entrySet() {
        return fields.entrySet();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }


    public int size() {
        return fields.size();
    }


    public Collection<Object> values() {
        return fields.values();
    }


    public Map<? extends Field<?>, ?> fields() {
        return fields;
    }

    public static abstract class FieldObjectMapBuilder<C extends FieldObjectMap, B extends FieldObjectMapBuilder<C, B>> {
        private final Map<Field<?>, Object> fields = new LinkedHashMap<>();

        public B field(Field<?> field, Object value) {
            Objects.requireNonNull(field);

            this.fields.put(field, field.getType().parse(value));

            return self();
        }

        public B fields(Map<Field<?>, Object> fieldMap) {
            if (fieldMap == null) {
                return self();
            }

            return fields(fieldMap.entrySet());
        }

        public B fields(Iterable<Map.Entry<Field<?>, Object>> iterable) {
            if (iterable != null) {
                iterable.forEach(entry -> field(entry.getKey(), entry.getValue()));
            }

            return self();
        }

        public B fields(FieldObjectMap fields) {
            if (fields != null) {
                this.fields.putAll(fields.fields());
            }
            return self();
        }

        public B remove(Field<?> field) {
            fields.remove(field);

            return self();
        }

        public B remove(Collection<Field<?>> remove) {
            if (remove == null || remove.isEmpty()) {
                return self();
            }

            Set<Field<?>> fieldsToRemove = new HashSet<>(fields.keySet());
            fieldsToRemove.removeAll(remove);

            fieldsToRemove.forEach(field -> fields.remove(field));

            return self();
        }

        public B addChild(FieldObjectMap child) {
            ((List<FieldObjectMap>) fields.computeIfAbsent(Field.CHILDREN, k -> new ArrayList<FieldObjectMap>())).add(child);

            return self();
        }

        public B children(List<FieldObjectMap> children) {
            return field(Field.CHILDREN, children);
        }

        public B id(String id) {
            return field(Field.ID, id);
        }

        public B title(String title) {
            return field(Field.TITLE, title);
        }

        public B content(String content) {
            return field(Field.CONTENT, content);
        }

        public B createDatetime(Instant createDatetime) {
            return field(Field.CREATE_DATETIME, createDatetime);
        }

        public B modifyDatetime(Instant timestamp) {
            return field(Field.MODIFY_DATETIME, timestamp);
        }

        public B className(String className) {
            return field(Field.CLASS_NAME, className);
        }
    }
}
