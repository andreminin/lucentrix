package org.lucentrix.ingest;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.ingest.metadata.FieldObjectMap;
import org.lucentrix.ingest.metadata.HasId;
import org.lucentrix.ingest.metadata.field.Field;

import java.util.*;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SourceDocument extends FieldObjectMap implements HasId {

    public SourceDocument() {
        this(new LinkedHashMap<>());
    }

    public SourceDocument(Map<Field<?>, Object> fields) {
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

    public String getVersionSeriesId() {
        return get(Field.VERSION_SERIES_ID);
    }

    public static abstract class SourceDocumentBuilder<C extends SourceDocument, B extends SourceDocumentBuilder<C, B>>
            extends FieldObjectMapBuilder<C, B> {
        Map<Field<?>, Object> fields;

        public SourceDocumentBuilder() {
            fields = new LinkedHashMap<>();
        }

        private SourceDocumentBuilder(Map<Field<?>, Object> fields) {
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
