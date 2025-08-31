package org.lucentrix.metaframe.persistence.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lucentrix.metaframe.metadata.Cursor;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;
import org.lucentrix.metaframe.runtime.persistence.FSJsonPersistence;

import java.io.File;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


public class FSCursorPersistenceTest {

    @TempDir
    File tempDir;

    @Test
    public void testCursorPersistence() {
        FSJsonPersistence persistence = new FSJsonPersistence(tempDir.toPath());

        assertTrue(this.tempDir.isDirectory());

        String cursorName = "cursor";
        Cursor original = Cursor.builder().id(cursorName)
                .fields(FieldObjectMap.builder()
                        .field(Field.CLASS_NAME, "Cursor")
                        .field(Field.CREATE_DATETIME, Instant.now())
                        .field(Field.MODIFY_DATETIME, Instant.now())
                        .field(Field.of("offset", FieldType.LONG), 123L)
                        .field(Field.CONTENT, "234399%$(*#$767@!#@#")
                        .build()).build();
        Cursor.SuffixCursor suffixCursor = Cursor.SuffixCursor.toSuffixCursor(original);

        persistence.save(suffixCursor);

        File jsonFile = new File(tempDir.getAbsolutePath() + File.separator + cursorName + ".json");
        assertTrue(jsonFile.exists());

        suffixCursor = persistence.load(cursorName, Cursor.SuffixCursor.class);
        Cursor restored = Cursor.SuffixCursor.toCursor(suffixCursor);

        assertNotNull(restored);

        assertEquals(original, restored);
    }
}
