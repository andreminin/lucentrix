package org.lucentrix.metaframe.metadata.converter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.StringObjectMap;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.mapping.FieldSerde;
import org.lucentrix.metaframe.metadata.mapping.SuffixSerde;

import java.util.Map;

public class SuffixObjectMapper {

    FieldToSuffixSchemaConverter toSuffixSchemaConverter;
    SuffixToFieldSchemaConverter toFieldSchemaConverter;

    public SuffixObjectMapper() {
        this(FieldSerde.SUFFIX_MAPPING);
    }

    public SuffixObjectMapper(SuffixSerde suffixSchema) {
        this.toSuffixSchemaConverter = new FieldToSuffixSchemaConverter(suffixSchema);
        this.toFieldSchemaConverter = new SuffixToFieldSchemaConverter(suffixSchema);
    }

    public StringObjectMap convert(FieldObjectMap input) {
        return toSuffixSchemaConverter.convert(input);
    }

    public FieldObjectMap convert(StringObjectMap input) {
        return toFieldSchemaConverter.convert(input);
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class FieldToSuffixSchemaConverter implements ObjectSchemaConverter<FieldObjectMap, StringObjectMap> {
        SuffixSerde suffixSchema;

        @Override
        public StringObjectMap convert(FieldObjectMap input) {
            StringObjectMap.StringObjectMapBuilder builder = StringObjectMap.builder();
            for(Map.Entry<Field<?>, Object> entry : input.entrySet()) {
                builder.set(suffixSchema.toString(entry.getKey()), entry.getValue());
            }

            return builder.build();
        }
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class SuffixToFieldSchemaConverter implements ObjectSchemaConverter<StringObjectMap, FieldObjectMap> {
        SuffixSerde suffixSchema;

        @Override
        public FieldObjectMap convert(StringObjectMap input) {
            FieldObjectMap.FieldObjectMapBuilder builder = FieldObjectMap.builder();

            for(Map.Entry<String, Object> entry : input.entrySet()) {
                builder.field(suffixSchema.fromString(entry.getKey()), entry.getValue());
            }

            return builder.build();
        }
    }

}
