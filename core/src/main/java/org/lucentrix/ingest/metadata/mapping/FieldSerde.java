package org.lucentrix.ingest.metadata.mapping;


import org.lucentrix.ingest.metadata.field.Field;

import java.io.Serializable;


public interface FieldSerde extends Serializable {

    PlainSerde PLAIN_MAPPING = new PlainSerde();

    SuffixSerde SUFFIX_MAPPING = new SuffixSerde();

    String toString(Field<?> field);

    Field<?> fromString(String fieldSignature);

    default Field<?> fromString(String fieldSignature, Object detectFromValue) {
        return fromString(fieldSignature);
    }


}
