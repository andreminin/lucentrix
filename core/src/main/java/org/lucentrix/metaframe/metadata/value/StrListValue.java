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
public class StrListValue implements FieldValue<List<String>> {
    List<String> value;

    public StrListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.STRING;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseStrings(value);
    }

    @Override
    public List<String> get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
