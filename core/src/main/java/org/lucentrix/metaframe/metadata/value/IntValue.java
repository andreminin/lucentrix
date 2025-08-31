package org.lucentrix.metaframe.metadata.value;


import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class IntValue implements FieldValue<Integer> {
	Integer value;

	public IntValue(Object value) {
		set(value);
	}

	@Override
	public TypeId getTypeId() {
		return TypeId.INT;
	}

	@Override
	public void set(Object value) {
		this.value = FieldTypeUtil.parseInteger(value);
	}

	@Override
	public Integer get() {
		return value;
	}

	@Override
	public boolean isMultiValue() {
		return false;
	}
}
