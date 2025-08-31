package org.lucentrix.metaframe.metadata.field;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.StringObjectMap;
import org.lucentrix.metaframe.metadata.StringValueMap;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;


@Getter
@EqualsAndHashCode
@ToString
public class FieldType<T> {
    private final static FieldType<?>[] TYPES;

    // Single value types
    public final static FieldType<String> STRING = new FieldType<>(TypeId.STRING, String.class, FieldTypeUtil::parseString);
    public final static FieldType<Double> DOUBLE = new FieldType<>(TypeId.DOUBLE, Double.class, FieldTypeUtil::parseDouble);
    public final static FieldType<Boolean> BOOLEAN = new FieldType<>(TypeId.BOOLEAN, Boolean.class, FieldTypeUtil::parseBoolean);
    public final static FieldType<Instant> DATETIME = new FieldType<>(TypeId.DATETIME, Instant.class, FieldTypeUtil::parseInstant);
    public final static FieldType<Long> LONG = new FieldType<>(TypeId.LONG, Long.class, FieldTypeUtil::parseLong);
    public final static FieldType<Integer> INT = new FieldType<>(TypeId.INT, Integer.class, FieldTypeUtil::parseInteger);
    public final static FieldType<Float> FLOAT = new FieldType<>(TypeId.FLOAT, Float.class, FieldTypeUtil::parseFloat);
    public final static FieldType<byte[]> BYTES = new FieldType<>(TypeId.BYTES, byte[].class, FieldTypeUtil::parseByteArray);
    public final static FieldType<java.util.UUID> UUID = new FieldType<>(TypeId.UUID, java.util.UUID.class, FieldTypeUtil::parseUUID);

    public final static FieldType<FieldObjectMap> FIELD_OBJECT_MAP = new FieldType<>(TypeId.FIELD_OBJECT_MAP, FieldObjectMap.class, FieldTypeUtil::parseFieldObjectMap);
    public final static FieldType<StringObjectMap> STRING_OBJECT_MAP = new FieldType<>(TypeId.STRING_OBJECT_MAP, StringObjectMap.class, FieldTypeUtil::parseStringObjectMap);
    public final static FieldType<StringValueMap> STRING_VALUE_MAP = new FieldType<>(TypeId.STRING_VALUE_MAP, StringValueMap.class, FieldTypeUtil::parseStringValueMap);

    // Multi-value types
    public final static FieldType<List<String>> STRING_LIST = new FieldType<>(TypeId.STRING_LIST, String.class, List.class, FieldTypeUtil::parseStrings);
    public final static FieldType<List<Long>> LONG_LIST = new FieldType<>(TypeId.LONG_LIST, Long.class, List.class, FieldTypeUtil::parseLongs);
    public final static FieldType<List<Double>> DOUBLE_LIST = new FieldType<>(TypeId.DOUBLE_LIST, Double.class, List.class, FieldTypeUtil::parseDoubles);
    public final static FieldType<List<Boolean>> BOOLEAN_LIST = new FieldType<>(TypeId.BOOLEAN_LIST, Boolean.class, List.class, FieldTypeUtil::parseBooleans);
    public final static FieldType<List<Instant>> DATETIME_LIST = new FieldType<>(TypeId.DATETIME_LIST, Instant.class, List.class, FieldTypeUtil::parseDates);
    public final static FieldType<List<Integer>> INT_LIST = new FieldType<>(TypeId.INT_LIST, Integer.class, List.class, FieldTypeUtil::parseIntegers);
    public final static FieldType<List<Float>> FLOAT_LIST = new FieldType<>(TypeId.FLOAT_LIST, Float.class, List.class, FieldTypeUtil::parseFloats);
    public final static FieldType<List<byte[]>> BYTES_LIST = new FieldType<>(TypeId.BYTES_LIST, byte[].class, List.class, FieldTypeUtil::parseByteArrays);
    public final static FieldType<List<java.util.UUID>> UUID_LIST = new FieldType<>(TypeId.UUID_LIST, java.util.UUID.class, List.class, FieldTypeUtil::parseUUIDs);

    public final static FieldType<List<FieldObjectMap>> FIELD_OBJECT_MAPS = new FieldType<>(TypeId.FIELD_OBJECT_MAPS, FieldObjectMap.class, List.class, FieldTypeUtil::parseFieldObjectMaps);
    public final static FieldType<List<StringObjectMap>> STRING_OBJECT_MAPS = new FieldType<>(TypeId.STRING_OBJECT_MAPS, FieldObjectMap.class, List.class, FieldTypeUtil::parseStringObjectMaps);
    public final static FieldType<List<StringValueMap>> STRING_VALUE_MAPS = new FieldType<>(TypeId.STRING_VALUE_MAPS, FieldObjectMap.class, List.class, FieldTypeUtil::parseStringValueMaps);


    static {
        TYPES = new FieldType[TypeId.values().length];
        TYPES[TypeId.STRING.getId()] = FieldType.STRING;
        TYPES[TypeId.BOOLEAN.getId()] = FieldType.BOOLEAN;
        TYPES[TypeId.INT.getId()] = FieldType.INT;
        TYPES[TypeId.DOUBLE.getId()] = FieldType.DOUBLE;
        TYPES[TypeId.FLOAT.getId()] = FieldType.FLOAT;
        TYPES[TypeId.LONG.getId()] = FieldType.LONG;
        TYPES[TypeId.UUID.getId()] = FieldType.UUID;
        TYPES[TypeId.BYTES.getId()] = FieldType.BYTES;
        TYPES[TypeId.DATETIME.getId()] = FieldType.DATETIME;

        TYPES[TypeId.STRING_LIST.getId()] = FieldType.STRING_LIST;
        TYPES[TypeId.BOOLEAN_LIST.getId()] = FieldType.BOOLEAN_LIST;
        TYPES[TypeId.INT_LIST.getId()] = FieldType.INT_LIST;
        TYPES[TypeId.DOUBLE_LIST.getId()] = FieldType.DOUBLE_LIST;
        TYPES[TypeId.FLOAT_LIST.getId()] = FieldType.FLOAT_LIST;
        TYPES[TypeId.LONG_LIST.getId()] = FieldType.LONG_LIST;
        TYPES[TypeId.UUID_LIST.getId()] = FieldType.UUID_LIST;
        TYPES[TypeId.BYTES_LIST.getId()] = FieldType.BYTES_LIST;
        TYPES[TypeId.DATETIME_LIST.getId()] = FieldType.DATETIME_LIST;

        TYPES[TypeId.FIELD_OBJECT_MAP.getId()] = FieldType.FIELD_OBJECT_MAP;
        TYPES[TypeId.STRING_OBJECT_MAP.getId()] = FieldType.STRING_OBJECT_MAP;
        TYPES[TypeId.STRING_VALUE_MAP.getId()] = FieldType.STRING_VALUE_MAP;

        TYPES[TypeId.FIELD_OBJECT_MAPS.getId()] = FieldType.FIELD_OBJECT_MAPS;
        TYPES[TypeId.STRING_OBJECT_MAPS.getId()] = FieldType.STRING_OBJECT_MAPS;
        TYPES[TypeId.STRING_VALUE_MAPS.getId()] = FieldType.STRING_VALUE_MAPS;
    }

    private final TypeId typeId;
    private final Class<?> valueClass;
    private final Class<?> cardinalityClass;
    private final boolean multivalued;
    private final Function<Object, T> valueParser;

    private FieldType(TypeId typeId, Class<T> valueClass, Function<Object, T> valueParser) {
        this(typeId, valueClass, null, valueParser);
    }

    public static <T> FieldType<T> of(TypeId typeId) {
        if(typeId == null) {
            return null;
        }

        return (FieldType<T>) TYPES[typeId.getId()];
    }

    public FieldType(TypeId typeId, Class<?> valueClass, Class<?> cardinalityClass, Function<Object, T> valueParser) {
        if (valueClass == null) {
            throw new IllegalArgumentException("Value class is null!");
        }
        if (cardinalityClass != null && !(Collection.class.isAssignableFrom(cardinalityClass) || cardinalityClass.isArray())) {
            throw new IllegalArgumentException("Cardinality class number be array or Collection!");
        }

        this.typeId = typeId;
        this.valueClass = valueClass;
        this.cardinalityClass = cardinalityClass;
        this.valueParser = valueParser;
        this.multivalued = cardinalityClass != null && valueClass != cardinalityClass;

    }

    public T parse(Object value) {
        return valueParser.apply(value);
    }

    public boolean isDateType() {
        return Instant.class.isAssignableFrom(valueClass) || Date.class.isAssignableFrom(valueClass);
    }

    public static FieldType<?> detectByValue(Object value) {
        if (value == null) {
            return FieldType.STRING;
        } else {
            Class<?> valueClass = value.getClass();
            if (valueClass.isArray()) {
                // Detected array
                return detectByClass(valueClass.getComponentType());
            } else if (Collection.class.isAssignableFrom(valueClass)) {
                Collection values = (Collection) value;
                return values.isEmpty() ? FieldType.STRING_LIST : detectByValue(values.iterator().next());
            } else {
                return detectByClass(valueClass);
            }
        }
    }

    private static FieldType<?> detectByClass(Class<?> valueClass) {
        if (String.class.isAssignableFrom(valueClass)) {
            return FieldType.STRING;
        } else if (Integer.class.isAssignableFrom(valueClass)) {
            return FieldType.INT;
        } else if (Long.class.isAssignableFrom(valueClass)) {
            return FieldType.LONG;
        } else if (Float.class.isAssignableFrom(valueClass)) {
            return FieldType.FLOAT;
        } else if (Double.class.isAssignableFrom(valueClass)) {
            return FieldType.DOUBLE;
        } else if (byte[].class.isAssignableFrom(valueClass)) {
            return FieldType.BYTES;
        } else if (UUID.class.isAssignableFrom(valueClass)) {
            return FieldType.UUID;
        } else if (Boolean.class.isAssignableFrom(valueClass)) {
            return FieldType.BOOLEAN;
        } else if (Date.class.isAssignableFrom(valueClass) || Instant.class.isAssignableFrom(valueClass)) {
            return FieldType.DATETIME;
        }

        return FieldType.STRING;
    }
}

