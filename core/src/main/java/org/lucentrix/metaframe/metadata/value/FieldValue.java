package org.lucentrix.metaframe.metadata.value;

import org.lucentrix.metaframe.metadata.field.TypeId;

import java.io.Serializable;


public interface FieldValue<T> extends Serializable {

    void set(Object value);

    T get();

    boolean isMultiValue();

    TypeId getTypeId();
}
