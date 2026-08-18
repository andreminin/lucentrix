package org.lucentrix.ingest.metadata.value;

import org.lucentrix.ingest.metadata.field.TypeId;

import java.io.Serializable;


public interface FieldValue<T> extends Serializable {

    void set(Object value);

    T get();

    boolean isMultiValue();

    TypeId getTypeId();
}
