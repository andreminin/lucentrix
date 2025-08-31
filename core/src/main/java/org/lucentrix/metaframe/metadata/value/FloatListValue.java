package org.lucentrix.metaframe.metadata.value;

import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.util.List;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class FloatListValue implements FieldValue<List<Float>> {
    List<Float> value;

    public FloatListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.FLOAT;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseFloats(value);
    }

    @Override
    public List<Float> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
