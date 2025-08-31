package org.lucentrix.metaframe.metadata.mapping;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.BiMap;

import java.util.Locale;
import java.util.Objects;


@ToString
public class SuffixSerde implements FieldSerde {

    public final static String SEPARATOR = "_";

    private static final SuffixTypeMapping DEFAULT_MAPPING = SuffixTypeMapping.builder()
            .types(BiMap.<String, TypeId>builder()
                    // Using SOLR suffix convention
                    .put("s", TypeId.STRING)
                    .put("ss", TypeId.STRING_LIST)
                    .put("i", TypeId.INT)
                    .put("is", TypeId.INT_LIST)
                    .put("l", TypeId.LONG)
                    .put("ls", TypeId.LONG_LIST)
                    .put("b", TypeId.BOOLEAN)
                    .put("bs", TypeId.BOOLEAN_LIST)
                    .put("f", TypeId.FLOAT)
                    .put("fs", TypeId.FLOAT_LIST)
                    .put("d", TypeId.DOUBLE)
                    .put("ds", TypeId.DOUBLE_LIST)
                    .put("dt", TypeId.DATETIME)
                    .put("dts", TypeId.DATETIME_LIST)
                    .put("obj", TypeId.FIELD_OBJECT_MAP)
                    .put("objs", TypeId.FIELD_OBJECT_MAPS)
                    .put("vmap", TypeId.STRING_VALUE_MAP)
                    .put("vmaps", TypeId.STRING_VALUE_MAPS)
                    .put("map", TypeId.STRING_OBJECT_MAP)
                    .put("maps", TypeId.STRING_OBJECT_MAPS)

                    .build())
            .build();

    private final TypeId defaultType;

    private final String defaultSuffix;

    @Builder.Default
    private SuffixTypeMapping suffixMapping = DEFAULT_MAPPING;

    public final static SuffixSerde INSTANCE = new SuffixSerde();

    public SuffixSerde() {
        this(DEFAULT_MAPPING, TypeId.STRING);
    }

    public SuffixSerde(SuffixTypeMapping suffixMapping) {
        this(suffixMapping, TypeId.STRING);
    }

    public SuffixSerde(SuffixTypeMapping suffixMapping, TypeId defaultType) {
        this.suffixMapping = suffixMapping;
        this.defaultType = defaultType;
        this.defaultSuffix = suffixMapping.suffixFromType(defaultType);
    }

    @Override
    public String toString(Field<?> field) {
        Objects.requireNonNull(field);

        return field.getName() + SEPARATOR + getSuffix(field.getType().getTypeId());
    }

    @Override
    public Field<?> fromString(String textField) {
        Objects.requireNonNull(textField);

        FieldType<?> fieldType = resolveType(textField);
        String baseName = getBaseName(textField);

        return Field.of(baseName, fieldType);
    }

    public FieldType<?> resolveType(String fieldName) {
        Objects.requireNonNull(fieldName);

        return FieldType.of(suffixMapping.typeFromSuffix(getSuffix(fieldName), defaultType));
    }

    public TypeId resolveTypeId(String fieldName) {
        Objects.requireNonNull(fieldName);

        return suffixMapping.typeFromSuffix(getSuffix(fieldName), defaultType);
    }

    public String getSuffix(TypeId fieldType) {
        Objects.requireNonNull(fieldType);

        return suffixMapping.suffixFromType(fieldType, defaultSuffix);
    }

    public String getSuffix(String fieldName) {
        int i = fieldName.lastIndexOf(SEPARATOR);

        if (i < 0 || i == fieldName.length() - 1) {
            return null;
        } else {
            return fieldName.substring(i + 1).toLowerCase(Locale.ROOT);
        }
    }

    public String getBaseName(String fieldName) {
        if (fieldName == null) {
            return null;
        }

        int index = fieldName.lastIndexOf(SEPARATOR);

        if (index > 0) {
            return fieldName.substring(0, index);
        } else {
            return fieldName;
        }
    }

    @Builder(toBuilder = true)
    @ToString
    @EqualsAndHashCode
    static class SuffixTypeMapping {
        BiMap<String, TypeId> types;

        public TypeId typeFromSuffix(String suffix, TypeId defaultType) {

            return types.getValueOrDefault(suffix, defaultType);
        }

        public String suffixFromType(TypeId type) {

            return types.getKey(type);
        }

        public String suffixFromType(TypeId type, String defaultSuffix) {

            return types.getKeyOrDefault(type, defaultSuffix);
        }
    }
}
