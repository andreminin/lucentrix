package org.lucentrix.metaframe.metadata.value;


import lombok.*;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.util.FieldTypeUtil;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString

@Builder(toBuilder = true)
public class UuidValue implements FieldValue<UUID> {
	UUID value;

	public UuidValue(Object value) {
		set(value);
	}

	@Override
	public TypeId getTypeId() {
		return TypeId.UUID;
	}

	@Override
	public void set(Object value) {
		this.value = FieldTypeUtil.parseUUID(value);
	}

	@Override
	public UUID get() {
		return value;
	}

	@Override
	public boolean isMultiValue() {
		return false;
	}
}
