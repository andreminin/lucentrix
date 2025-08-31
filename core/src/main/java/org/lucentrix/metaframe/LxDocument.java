package org.lucentrix.metaframe;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.Identity;
import org.lucentrix.metaframe.metadata.field.Field;

import java.util.*;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class LxDocument extends FieldObjectMap implements Identity {

    public LxDocument() {
        this(new LinkedHashMap<>());
    }

    public LxDocument(Map<Field<?>, Object> fields) {
        super(fields);
    }

    public String getMimeType() {
        return get(Field.MIME_TYPE);
    }

    public Boolean isCurrentVersion() {
        return get(Field.IS_CURRENT_VERSION);
    }

    public String getContentId() {
        return get(Field.CONTENT_ID);
    }

    public List<FieldObjectMap> getChildren() {
        return get(Field.CHILDREN);
    }

    public String getVersuibSeriesId() {
        return get(Field.VERSION_SERIES_ID);
    }

    public static abstract class LxDocumentBuilder<C extends LxDocument, B extends LxDocumentBuilder<C, B>>
            extends FieldObjectMapBuilder<C, B> {
        Map<Field<?>, Object> fields;

        public LxDocumentBuilder() {
            fields = new LinkedHashMap<>();
        }

        private LxDocumentBuilder(Map<Field<?>, Object> fields) {
            this.fields = new LinkedHashMap<>(fields);
        }

        public B vsId(String vsId) {
            return field(Field.VERSION_SERIES_ID, vsId);
        }

        public void currentVersion(boolean isCurrentVersion) {
            field(Field.IS_CURRENT_VERSION, isCurrentVersion);
        }

        public B mimeType(String mime) {
            return field(Field.MIME_TYPE, mime);
        }

    }
}
