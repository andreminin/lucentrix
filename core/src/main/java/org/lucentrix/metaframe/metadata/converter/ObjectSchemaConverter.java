package org.lucentrix.metaframe.metadata.converter;

public interface ObjectSchemaConverter<I, O> {

    O convert(I input);
}
