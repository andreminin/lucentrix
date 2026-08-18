package org.lucentrix.ingest.plugin.synanton;

import org.lucentrix.ingest.ChangeOp;
import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.SourceDocument;
import org.lucentrix.ingest.metadata.field.Field;
import org.lucentrix.ingest.serde.json.JacksonConfig;

import java.nio.charset.StandardCharsets;

final class SynantonPayload {

    private SynantonPayload() {
    }

    static byte[] bytes(SourceDocument document) {
        byte[] raw = document.get(Field.CONTENT_BYTES);
        if (raw != null && raw.length > 0) {
            return raw;
        }
        String text = document.get(Field.CONTENT);
        if (text != null) {
            return text.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return JacksonConfig.configureMapper().writeValueAsBytes(document);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize source document " + document.getId(), ex);
        }
    }

    static String mimeType(SourceDocument document) {
        String mime = document.getMimeType();
        if (mime != null && !mime.isBlank()) {
            return mime;
        }
        if (document.get(Field.CONTENT_BYTES) != null) {
            return "application/octet-stream";
        }
        if (document.get(Field.CONTENT) != null) {
            return "text/plain";
        }
        return "application/json";
    }

    static String sourceUri(SourceDocument document) {
        String uri = document.get(Field.SOURCE_URI);
        if (uri != null && !uri.isBlank()) {
            return uri;
        }
        String sourceId = document.get(Field.SOURCE_ID);
        if (sourceId != null && !sourceId.isBlank()) {
            return sourceId;
        }
        return "lucentrix://" + document.getId();
    }

    static boolean isDelete(ContentChange change) {
        return change != null && change.getAction() == ChangeOp.DELETE;
    }
}
