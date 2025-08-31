package org.lucentrix.metaframe.plugin.solr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.metadata.mapping.FieldSerde;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class SolrFieldSerde implements FieldSerde {

    public final static SolrFieldSerde INSTANCE = new SolrFieldSerde();

    public final static char[] CHARS_TO_ESCAPE;
    public final static char[] CHARS_ESCAPED;

    private final static Map<Character, Character> CHARS_ESCAPE_MAP;
    private final static Map<Character, Character> CHARS_ESCAPE_MAP_INVERTED;

    private final static Map<String, TypeId> SOLR_SYSTEM_FIELD_NAMES = Map.ofEntries(
            Map.entry("id", TypeId.STRING),
            Map.entry("_version_", TypeId.LONG),
            Map.entry("_root_", TypeId.STRING),
            Map.entry("_text_", TypeId.STRING)
    );

    private final static Set<Character> PROHIBITED_CHARS;

    static {
        CHARS_TO_ESCAPE = new char[]{'@', '\\', ':', '$', '!', '/', ';', '-', '*', '~', ' ', '.'};
        CHARS_ESCAPED = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'i', 'm', 's', 't', 'v', 'x'};
        PROHIBITED_CHARS = new HashSet<>();

        CHARS_ESCAPE_MAP = new HashMap<>();
        for (int i = 0; i < CHARS_TO_ESCAPE.length; i++) {
            CHARS_ESCAPE_MAP.put(CHARS_TO_ESCAPE[i], CHARS_ESCAPED[i]);
            PROHIBITED_CHARS.add(CHARS_TO_ESCAPE[i]);
        }
        PROHIBITED_CHARS.remove('_');

        CHARS_ESCAPE_MAP_INVERTED = new HashMap<>();
        for (int i = 0; i < CHARS_TO_ESCAPE.length; i++) {
            CHARS_ESCAPE_MAP_INVERTED.put(CHARS_ESCAPED[i], CHARS_TO_ESCAPE[i]);
        }
    }

    private final static String FIELD_TYPE_SEPARATOR = "a_";
    private final static int TYPE_SEPARATOR_LENGTH = FIELD_TYPE_SEPARATOR.length();

    private static final Pattern BOOLEAN_PATTERN = Pattern.compile(".*_b$");
    private static final Pattern STRING_PATTERN = Pattern.compile(".*_s$");
    private static final Pattern DOUBLE_PATTERN = Pattern.compile(".*_d$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile(".*_f$");
    private static final Pattern LONG_PATTERN = Pattern.compile(".*_l$");
    private static final Pattern BYTES_PATTERN = Pattern.compile(".*_bin$");
    private static final Pattern DATETIME_PATTERN = Pattern.compile(".*_dt$");
    private static final Pattern STRING_LIST_PATTERN = Pattern.compile(".*_ss$");
    private static final Pattern BOOLEAN_LIST_PATTERN = Pattern.compile(".*_bs$");
    private static final Pattern INT_LIST_PATTERN = Pattern.compile(".*_is$");
    private static final Pattern DOUBLE_LIST_PATTERN = Pattern.compile(".*_ds$");
    private static final Pattern FLOAT_LIST_PATTERN = Pattern.compile(".*_fs$");
    private static final Pattern LONG_LIST_PATTERN = Pattern.compile(".*_ls$");
    private static final Pattern DATETIME_LIST_PATTERN = Pattern.compile(".*_dts$");

    private final static Map<TypeId, String> TYPE_TO_SUFFIX = Map.ofEntries(
            Map.entry(TypeId.STRING, "_s"),
            Map.entry(TypeId.BOOLEAN, "_b"),
            Map.entry(TypeId.INT, "_i"),
            Map.entry(TypeId.DOUBLE, "_d"),
            Map.entry(TypeId.FLOAT, "_f"),
            Map.entry(TypeId.LONG, "_l"),
            Map.entry(TypeId.BYTES, "_bin"),
            Map.entry(TypeId.DATETIME, "_dt"),
            Map.entry(TypeId.STRING_LIST, "_ss"),
            Map.entry(TypeId.BOOLEAN_LIST, "_bs"),
            Map.entry(TypeId.INT_LIST, "_is"),
            Map.entry(TypeId.DOUBLE_LIST, "_ds"),
            Map.entry(TypeId.FLOAT_LIST, "_fs"),
            Map.entry(TypeId.LONG_LIST, "_ls"),
            Map.entry(TypeId.DATETIME_LIST, "_dts")
    );

    private final ConcurrentHashMap<String, Field<?>> reverseMapping;
    private final ConcurrentHashMap<Field<?>, String> mapping;

    public SolrFieldSerde() {
        this(null);
    }

    public SolrFieldSerde(Map<Field<?>, String> mapping) {
        this.mapping = mapping == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(mapping);

        this.reverseMapping = new ConcurrentHashMap<>(this.mapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
    }

    @Override
    public String toString(Field<?> field) {

        return this.mapping.computeIfAbsent(field, fld -> {
            String name = fld.getName();

            if (StringUtils.isBlank(name)) {
                return name;
            }

            TypeId typeId = SOLR_SYSTEM_FIELD_NAMES.get(name);
            if (typeId != null) {
                return name;
            }

            char ch;
            int length = name.length();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < length; i++) {
                ch = name.charAt(i);
                if (!Character.isLetterOrDigit(ch)) {
                    if (CHARS_ESCAPE_MAP.containsKey(ch)) {
                        if ('\\' == ch && i < length - 1 && CHARS_ESCAPE_MAP.containsKey(name.charAt(i + 1))) {
                            sb.append(name.charAt(i + 1));
                            i++;
                        } else {
                            sb.append(CHARS_ESCAPE_MAP.get(ch)).append('_');
                        }
                    } else {
                        sb.append(ch);
                    }
                } else {
                    sb.append(ch);
                }
            }

            typeId = fld.getType().getTypeId();
            String typeSuffix = TYPE_TO_SUFFIX.get(typeId);
            if(typeSuffix != null) {
                sb.append(typeSuffix);
            }

            String solrField = sb.toString();

            this.reverseMapping.put(solrField, fld);

            return solrField;
        });

    }

    @Override
    public Field<?> fromString(String solrField) {
        if (StringUtils.isBlank(solrField)) {
            return null;
        }
        TypeId systemType = SOLR_SYSTEM_FIELD_NAMES.get(solrField);
        if (systemType != null) {
            return Field.of(solrField, systemType);
        }

        return reverseMapping.computeIfAbsent(solrField, solrFld -> {
            TypeId typeId = detectType(solrField);

            String solrBaseName = getSolrFieldBase(solrField);

            return Field.of(solrBaseName, typeId);
        });
    }

    private String getSolrFieldBase(String solrField) {

        int index = solrField.lastIndexOf(FIELD_TYPE_SEPARATOR);
        if (index > 0) {
            return solrField.substring(0, index + FIELD_TYPE_SEPARATOR.length());
        } else {
            return solrField;
        }
    }

    private TypeId detectType(String solrName) {
        if (STRING_PATTERN.matcher(solrName).matches()) {
            return TypeId.STRING;
        } else if (BOOLEAN_PATTERN.matcher(solrName).matches()) {
            return TypeId.BOOLEAN;
        } else if (DOUBLE_PATTERN.matcher(solrName).matches()) {
            return TypeId.DOUBLE;
        } else if (DATETIME_PATTERN.matcher(solrName).matches()) {
            return TypeId.DATETIME;
        } else if (LONG_PATTERN.matcher(solrName).matches()) {
            return TypeId.LONG;
        } else if (FLOAT_PATTERN.matcher(solrName).matches()) {
            return TypeId.FLOAT;
        } else if (BYTES_PATTERN.matcher(solrName).matches()) {
            return TypeId.BYTES;
        }

        if (STRING_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.STRING_LIST;
        } else if (DATETIME_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.DATETIME_LIST;
        } else if (INT_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.INT_LIST;
        } else if (BOOLEAN_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.BOOLEAN_LIST;
        } else if (BYTES_PATTERN.matcher(solrName).matches()) {
            return TypeId.BYTES;
        } else if (DOUBLE_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.DOUBLE_LIST;
        } else if (FLOAT_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.FLOAT_LIST;
        } else if (LONG_LIST_PATTERN.matcher(solrName).matches()) {
            return TypeId.LONG_LIST;
        }

        //Default
        return TypeId.STRING;
    }

}
