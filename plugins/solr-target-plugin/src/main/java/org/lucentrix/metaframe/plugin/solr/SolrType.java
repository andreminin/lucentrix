package org.lucentrix.metaframe.plugin.solr;

public enum SolrType {
    STRING(1),
    LONG(2),
    DATE(3),
    DOUBLE(4),
    BOOLEAN(5),
    TEXT(6),
    DATETIME(7),
    INT(8),
    FLOAT(9),
    BINARY(10),
    UUID(11),
    OBJECT(12),
    UNKNOWN(-1);

    private final int value;

    SolrType(final int value) {
        this.value = value;
    }

    public int getValue() { return value; }
}
