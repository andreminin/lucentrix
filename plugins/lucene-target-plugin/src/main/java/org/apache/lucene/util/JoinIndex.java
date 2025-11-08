package org.apache.lucene.util;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;

import java.io.IOException;

public interface JoinIndex {
    void build(IndexReader leftIndex, IndexReader rightIndex) throws IOException;

    DocIdSet getMatchingDocs(IndexReader leftIndex, int docId, String fromField, String toField) throws IOException;

    boolean isBuilt();

    void clear();

    long getMemoryUsage();

    JoinIndexType getType();
}