package org.lucentrix.ingest.metadata.value;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.ingest.metadata.field.TypeId;
import org.lucentrix.ingest.util.FieldTypeUtil;

@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class ByteArrayValue implements FieldValue<byte[]> {
    byte[] value;

    public ByteArrayValue() {
    }

    public ByteArrayValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.BYTES;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseByteArray(value);
    }

    @Override
    public byte[] get() {
        return value;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }
}
