package org.lucentrix.metaframe.metadata.value;


import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.util.List;
import java.util.UUID;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class UuidListValue implements FieldValue<List<UUID>> {
    List<UUID> value;

    public UuidListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.UUID;
    }

    @Override
    public void set(Object value) {
        FieldTypeUtil.parseUUIDs(value);
    }

    @Override
    public List<UUID> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
