package org.lucentrix.metaframe.metadata;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@ToString
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class CursorItems<T> {
    Cursor cursor;
    List<T> items;
    boolean hasNext;

    public boolean hasNext() {
        return hasNext;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
