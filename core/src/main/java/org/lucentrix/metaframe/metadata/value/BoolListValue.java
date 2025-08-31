package org.lucentrix.metaframe.metadata.value;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.util.List;

@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class BoolListValue implements FieldValue<List<Boolean>> {
    List<Boolean> value;

    public BoolListValue(Object value) {
        set(value);
    }

    @Override
    public void set(Object value) {
        FieldTypeUtil.parseBooleans(value);
    }

    @Override
    public List<Boolean> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.BOOLEAN;
    }


}
