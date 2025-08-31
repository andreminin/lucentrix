package org.lucentrix.metaframe.metadata.value;


import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.time.Instant;
import java.util.List;



@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DateListValue implements FieldValue<List<Instant>> {
    List<Instant> value;

    public DateListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.DATETIME;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseDates(value);
    }

    @Override
    public List<Instant> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
