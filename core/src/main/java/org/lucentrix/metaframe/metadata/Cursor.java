package org.lucentrix.metaframe.metadata;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.metadata.converter.SuffixObjectMapper;

@ToString
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Cursor implements Identity {
    final String id;
    @Builder.Default
    final FieldObjectMap fields = new FieldObjectMap();


    public Cursor(String id) {
        this(id, new FieldObjectMap());
    }



    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder(toBuilder = true)
    public static class SuffixCursor implements Identity {
        private final static SuffixObjectMapper suffixObjectMapper = new SuffixObjectMapper();

        String id;

        StringObjectMap fields;

        public static SuffixCursor toSuffixCursor(Cursor cursor) {
            if (cursor == null) {
                return null;
            }

            return SuffixCursor.builder().id(cursor.getId())
                    .fields(suffixObjectMapper.convert(cursor.getFields())).build();
        }

        public static Cursor toCursor(SuffixCursor suffixCursor) {
            if (suffixCursor == null) {
                return null;
            }

            return Cursor.builder().id(suffixCursor.getId())
                    .fields(suffixObjectMapper.convert(suffixCursor.getFields())).build();
        }

    }
}
