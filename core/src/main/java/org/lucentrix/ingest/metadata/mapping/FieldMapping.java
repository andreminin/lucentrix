package org.lucentrix.ingest.metadata.mapping;

public interface FieldMapping<N, M> {
    N getInternalField();

    M getExternalField();

    default Direction getDirection() {
        return Direction.BOTH;
    }

    enum Direction {
        IN,
        OUT,
        BOTH
    }
}
