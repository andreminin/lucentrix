package org.lucentrix.metaframe.metadata.value;

import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.util.List;

@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DoubleListValue implements FieldValue<List<Double>> {
    List<Double> value;

    public DoubleListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.DOUBLE;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseDoubles(value);
    }

    @Override
    public List<Double> get() {
        return null;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
