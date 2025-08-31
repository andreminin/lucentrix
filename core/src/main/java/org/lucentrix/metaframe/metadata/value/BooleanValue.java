package org.lucentrix.metaframe.metadata.value;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class BooleanValue implements FieldValue<Boolean> {
    Boolean value;

    public BooleanValue(Object value) {
        set(value);
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseBoolean(value);
    }

    @Override
    public Boolean get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.BOOLEAN;
    }
}
