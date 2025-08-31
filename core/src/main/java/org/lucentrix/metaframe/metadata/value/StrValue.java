package org.lucentrix.metaframe.metadata.value;


import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;


@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
public class StrValue implements FieldValue<String> {
    String value;

    public StrValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.STRING;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseString(value);
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }
}
