package org.lucentrix.metaframe.metadata.field;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Getter
public enum TypeId {
    // Plain types
    STRING(0, "string"),
    BOOLEAN(1, "boolean"),
    INT(2, "int"),
    DOUBLE(3, "double"),
    FLOAT(4, "float"),
    LONG(5, "long"),
    UUID(6, "uuid"),
    BYTES(7, "byte_array"),
    DATETIME(8, "datetime"),

    STRING_LIST(9, "strings"),
    BOOLEAN_LIST(10, "booleans"),
    INT_LIST(11, "ints"),
    DOUBLE_LIST(12, "double"),
    FLOAT_LIST(13, "float"),
    LONG_LIST(14, "longs"),
    UUID_LIST(15, "uuids"),
    BYTES_LIST(16, "bytes_arrays"),
    DATETIME_LIST(17, "datetimes"),

    // Object types
    FIELD_OBJECT_MAP(18, "field_object_map"),
    STRING_OBJECT_MAP(19, "string_object_map"),
    STRING_VALUE_MAP(20, "string_value_map"),

    FIELD_OBJECT_MAPS(21, "field_object_maps"),
    STRING_OBJECT_MAPS(22, "string_object_maps"),
    STRING_VALUE_MAPS(23, "string_value_maps");

    private final static TypeId[] TYPES;

    static {
        TYPES = new TypeId[1+Arrays.stream(TypeId.values())
                .mapToInt(TypeId::getId)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("Array must not be empty"))];

        for (TypeId typeId : TypeId.values()) {
            TYPES[typeId.id] = typeId;
        }

        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == null) {
                throw new IllegalStateException("TypeId enumeration is not continues: missing id=" + i);
            }
        }
    }

    private final String name;
    private final int id;

    TypeId(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static TypeId fromId(int id) {
        if (id < 0 || id >= TYPES.length) {
            throw new IllegalArgumentException("Id is out of range 0-" + TYPES.length);
        }
        return TYPES[id];
    }


    public static TypeId parse(String value) {
        if (StringUtils.isNotBlank(value)) {
            value = StringUtils.lowerCase(StringUtils.trim(value));
            for (TypeId typeId : values()) {
                if (typeId.name.equals(value)) {
                    return typeId;
                }
            }
        }

        throw new IllegalArgumentException("Unrecognized enumeration value: " + value);
    }

    public String value() {
        return name;
    }
}