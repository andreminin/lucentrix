package org.lucentrix.metaframe.metadata.value;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DoubleValue implements FieldValue<Double> {
    Double value;

    public DoubleValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.DOUBLE;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseDouble(value);
    }

    @Override
    public Double get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }
}
