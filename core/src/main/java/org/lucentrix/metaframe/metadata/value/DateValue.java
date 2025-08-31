package org.lucentrix.metaframe.metadata.value;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.time.Instant;

@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DateValue implements FieldValue<Instant> {
    Instant value;

    public DateValue(Instant value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.DATETIME;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseInstant(value);
    }

    @Override
    public Instant get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }
}
