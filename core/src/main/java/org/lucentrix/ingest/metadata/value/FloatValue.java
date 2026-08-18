package org.lucentrix.ingest.metadata.value;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.ingest.metadata.field.TypeId;
import org.lucentrix.ingest.util.FieldTypeUtil;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class FloatValue implements FieldValue<Float> {
    Float value;

    public FloatValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.FLOAT;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseFloat(value);
    }

    @Override
    public Float get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }
}
