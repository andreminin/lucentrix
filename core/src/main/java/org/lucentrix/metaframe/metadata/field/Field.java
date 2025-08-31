package org.lucentrix.metaframe.metadata.field;

import lombok.*;
import org.lucentrix.metaframe.metadata.FieldObjectMap;

import java.time.Instant;
import java.util.*;


@Builder
@ToString
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class Field<T> implements Comparable<String> {

    private final String name;

    private final FieldType<T> type;

    public static <T> Field<T> of(String name, FieldType<T> type) {
        return new Field<>(name, type);
    }

    public static <T> Field<T> of(String name, TypeId typeId) {
        return new Field<>(name, FieldType.of(typeId));
    }

    public static Field<String> of(String name) {
        return new Field<>(name, FieldType.STRING);
    }

    // ID fields
    public static final Field<String> ID = new Field<>("id", FieldType.STRING);
    public static final Field<String> OID = new Field<>("o_id", FieldType.STRING);
    public static final Field<String> GUID = new Field<>("guid", FieldType.STRING);
    public static final Field<String> VERSION_SERIES_ID = new Field<>("vs_id", FieldType.STRING);

    public static final Field<String> TITLE = new Field<>("title", FieldType.STRING);
    public static final Field<Instant> MODIFY_DATETIME = new Field<>("date_modified", FieldType.DATETIME);
    public static final Field<Instant> CREATE_DATETIME = new Field<>("date_created", FieldType.DATETIME);
    public static final Field<Instant> ADDED_DATETIME = new Field<>("date_added", FieldType.DATETIME);
    public static final Field<String> CREATOR = new Field<>("creator", FieldType.STRING);
    public static final Field<String> LAST_MODIFIER = new Field<>("last_modifier", FieldType.STRING);

    public static final Field<String> CONTENT = new Field<>("content", FieldType.STRING);

    public static final Field<String> PARENT_ID = new Field<>("parent_id", FieldType.STRING);
    public static final Field<List<FieldObjectMap>> CHILDREN = new Field<>("children", FieldType.FIELD_OBJECT_MAPS);

    public static final Field<String> SOURCE_ID = new Field<>("source_id", FieldType.STRING);
    public static final Field<String> TARGET_ID = new Field<>("target_id", FieldType.STRING);

    public static final Field<String> CLASS_NAME = new Field<>("class_name", FieldType.STRING);

    public static final Field<Boolean> IS_FOLDER = new Field<>("is_folder", FieldType.BOOLEAN);

    public static final Field<String> FOLDER_PATH = new Field<>("folder_path", FieldType.STRING);
    public static final Field<List<String>> FOLDER_PATHS = new Field<>("folder_paths", FieldType.STRING_LIST);
    public static final Field<List<String>> PATHS = new Field<>("paths", FieldType.STRING_LIST);


    public static final Field<String> MIME_TYPE = new Field<>("mime_type", FieldType.STRING);
    public static final Field<String> DESCRIPTION = new Field<>("description", FieldType.STRING);


    public static final Field<String> CONTENT_ID = new Field<>("content_id", FieldType.STRING);
    public static final Field<String> CONTENT_STATUS = new Field<>("content_status", FieldType.STRING);
    public static final Field<Long> CONTENT_SIZE = new Field<>("content_size", FieldType.LONG);
    public static final Field<byte[]> CONTENT_BYTES = new Field<>("content_bytes", FieldType.BYTES);

    public static final Field<String> FILE_EXTENSION = new Field<>("file_extension", FieldType.STRING);

    public static final Field<String> VERSION_NUMBER = new Field<>("version", FieldType.STRING);
    public static final Field<Boolean> IS_CURRENT_VERSION = new Field<>("is_current_version", FieldType.BOOLEAN);
    public static final Field<Boolean> IS_RESERVED = new Field<>("is_reserved", FieldType.BOOLEAN);
    public static final Field<Boolean> IS_CURRENT = new Field<>("is_current", FieldType.BOOLEAN);
    public static final Field<Boolean> IS_COMPOUND = new Field<>("is_compound", FieldType.BOOLEAN);


    // Annotation fields
    public static final Field<Double> TOP = new Field<>("top", FieldType.DOUBLE);
    public static final Field<Double> WIDTH = new Field<>("width", FieldType.DOUBLE);
    public static final Field<Double> HEIGHT = new Field<>("height", FieldType.DOUBLE);
    public static final Field<Double> LEFT = new Field<>("left", FieldType.DOUBLE);
    public static final Field<String> TOOLTIP = new Field<>("tooltip", FieldType.STRING);
    public static final Field<String> TOOLTIP_ENCODING = new Field<>("tooltip_encoding", FieldType.STRING);
    public static final Field<Integer> PAGE_NUMBER = new Field<>("page_number", FieldType.INT);
    public static final Field<Integer> ANNOTATED_CONTENT_ELEMENT_IDX = new Field<>("annotated_content_element", FieldType.INT);


    @Override
    public int compareTo(String s) {
        return this.name.compareTo(s);
    }
}
