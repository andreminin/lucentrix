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
public class ByteArrayListValue implements FieldValue<List<byte[]>> {

    List<byte[]> value;

    public ByteArrayListValue(Object value) {
        set(value);
    }

    @Override
    public TypeId getTypeId() {
        return TypeId.BYTES_LIST;
    }

    @Override
    public void set(Object value) {
        this.value = FieldTypeUtil.parseByteArrays(value);
    }

    @Override
    public List<byte[]> get() {
        return null;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }
}
