package org.lucentrix.ingest.metadata.value;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.ingest.metadata.field.TypeId;
import org.lucentrix.ingest.util.FieldTypeUtil;

import java.util.List;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class IntListValue implements FieldValue<List<Integer>> {
    List<Integer> value;

    public IntListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.INT;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseIntegers(value);
    }

    @Override
    public List<Integer> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
