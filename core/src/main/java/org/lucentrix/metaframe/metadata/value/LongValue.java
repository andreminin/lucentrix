package org.lucentrix.metaframe.metadata.value;


import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class LongValue implements FieldValue<Long> {
	Long value;

	public LongValue(Object value) {
		set(value);
	}

	@Override
	public TypeId getTypeId() {
		return TypeId.LONG;
	}

	@Override
	public void set(Object value) {
		this.value = FieldTypeUtil.parseLong(value);
	}

	@Override
	public Long get() {
		return value;
	}

	@Override
	public boolean isMultiValue() {
		return false;
	}
}
