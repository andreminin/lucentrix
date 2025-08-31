package org.lucentrix.metaframe.metadata.value;


import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@ToString

@Builder(toBuilder = true)
public class LongListValue implements FieldValue<List<Long>> {
    List<Long> value;

    public LongListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.LONG;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseLongs(value);
    }

    @Override
    public List<Long> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
