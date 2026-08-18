package org.lucentrix.ingest.metadata.converter;

public interface ObjectSchemaConverter<I, O> {

    O convert(I input);
}
