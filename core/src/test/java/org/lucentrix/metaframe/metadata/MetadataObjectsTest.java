package org.lucentrix.metaframe.metadata;

import org.junit.jupiter.api.Test;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetadataObjectsTest {

    @Test
    public void testFieldObjectMap() {
        String id = UUID.randomUUID().toString();
        String oid = UUID.randomUUID().toString();
        Instant created = Instant.now().minus(Duration.ofDays(365));
        Instant modified = Instant.now();

        FieldObjectMap objectMap = FieldObjectMap.builder()
                .id(id)
                .field(Field.OID, oid)
                .className("Custom")
                .createDatetime(created)
                .modifyDatetime(modified)
                .title("Custom Object")
                .build();

        assertEquals(id, objectMap.getId());
        assertEquals(oid, objectMap.get(Field.OID));
        assertEquals("Custom", objectMap.getClassName());
        assertEquals(created, objectMap.getDateCreated());
        assertEquals(modified, objectMap.getModifyDatetime());
        assertEquals("Custom Object", objectMap.getTitle());
    }

    @Test
    public void testCursorDefault() {
        Cursor cursor = Cursor.builder().build();

        FieldObjectMap objectMap = cursor.getFields();

        assertNotNull(objectMap);
    }

    @Test
    public void testCursor() {
        String id = UUID.randomUUID().toString();
        Instant created = Instant.now().minus(Duration.ofDays(365));
        Instant modified = Instant.now();

        FieldObjectMap fields = FieldObjectMap.builder()
                .id(id)
                .field(Field.of("seed", FieldType.LONG), 10001L)
                .field(Field.of("security_count", FieldType.INT), 25)
                .className("Cursor")
                .createDatetime(created)
                .modifyDatetime(modified)
                .title("Dummy source cursor")
                .build();

        Cursor cursor = Cursor.builder().fields(fields).id("dummy_insurance").build();

        FieldObjectMap objectMap = cursor.getFields();

        assertEquals(id, objectMap.getId());
        assertEquals(10001L, objectMap.get(Field.of("seed", FieldType.LONG)));
        assertEquals(25, objectMap.get(Field.of("security_count", FieldType.INT)));
        assertEquals("Cursor", objectMap.getClassName());
        assertEquals(created, objectMap.getDateCreated());
        assertEquals(modified, objectMap.getModifyDatetime());
        assertEquals("Dummy source cursor", objectMap.getTitle());
    }

    @Test
    public void testDocument() {
        Instant childCreated = Instant.now().minus(Duration.ofDays(365));
        Instant childModified = Instant.now();

        FieldObjectMap child = FieldObjectMap.builder()
                .id("child_1")
                .field(Field.OID, "oid_1")
                .className("Child")
                .createDatetime(childCreated)
                .modifyDatetime(childModified)
                .title("Child 1")
                .build();

        String id = UUID.randomUUID().toString();
        String oid = UUID.randomUUID().toString();
        Instant created = Instant.now().minus(Duration.ofDays(365));
        Instant modified = Instant.now();

        LxDocument document = LxDocument.builder()
                .id(id)
                .field(Field.OID, oid)
                .className("Document")
                .createDatetime(created)
                .modifyDatetime(modified)
                .title("Document 1")
                .mimeType("application/pdf")
                .vsId("vs123")
                .addChild(child)
                .build();

        assertEquals(id, document.getId());
        assertEquals(oid, document.get(Field.OID));
        assertEquals("Document", document.getClassName());
        assertEquals(created, document.getDateCreated());
        assertEquals(modified, document.getModifyDatetime());
        assertEquals("Document 1", document.getTitle());
        assertEquals("application/pdf", document.getMimeType());
        assertEquals("vs123", document.getVersuibSeriesId());

        List<FieldObjectMap> children = document.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());

        assertEquals(child, children.get(0));
    }
}
