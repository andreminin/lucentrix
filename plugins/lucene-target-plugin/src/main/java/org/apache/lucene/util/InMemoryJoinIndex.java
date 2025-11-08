package org.apache.lucene.util;

import org.apache.lucene.index.*;
import org.apache.lucene.internal.hppc.IntArrayList;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemoryJoinIndex implements JoinIndex {
    private final Map<String, IntArrayList> leftToRightMap;
    private final Map<String, IntArrayList> rightToLeftMap;
    private boolean built = false;
    private final String leftField;
    private final String rightField;

    public InMemoryJoinIndex(String leftField,String rightField) {
        this.leftField = leftField;
        this.rightField = rightField;

        this.leftToRightMap = new HashMap<>();
        this.rightToLeftMap = new HashMap<>();
    }

    @Override
    public void build(IndexReader leftIndex, IndexReader rightIndex) throws IOException {
        clear();

        // Build left -> right mapping
        buildFieldMapping(leftIndex, leftField, leftToRightMap);

        // Build right -> left mapping
        buildFieldMapping(rightIndex, rightField, rightToLeftMap);

        built = true;
    }

    private void buildFieldMapping(IndexReader index, String field,
                                   Map<String, IntArrayList> mapping) throws IOException {
        Terms terms = MultiTerms.getTerms(index, field);
        if (terms == null) return;

        TermsEnum termsEnum = terms.iterator();
        BytesRef term;

        while ((term = termsEnum.next()) != null) {
            String termValue = term.utf8ToString();
            PostingsEnum postings = termsEnum.postings(null);
            IntArrayList docIds = new IntArrayList();

            int docId;
            while ((docId = postings.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                docIds.add(docId);
            }

            mapping.put(termValue, docIds);
        }
    }

    @Override
    public DocIdSet getMatchingDocs(IndexReader leftIndex, int docId, String fromField, String toField) throws IOException {
        if (!built) {
            throw new IllegalStateException("Index not built");
        }

        // Get field value from source document
        String fieldValue = getFieldValue(leftIndex, docId, fromField);
        if (fieldValue == null) {
            return null;
        }

        // Find matching documents in target field
        Map<String, IntArrayList> targetMap = fromField.equals(leftField) ? leftToRightMap : rightToLeftMap;
        IntArrayList matchingDocIds = targetMap.get(fieldValue);

        if (matchingDocIds == null || matchingDocIds.isEmpty()) {
            return null;
        }

        return new IntArrayDocIdSet(matchingDocIds.toArray(), matchingDocIds.size());
    }

    private String getFieldValue(IndexReader index, int docId, String fromField) throws IOException {
        //TODO review
        return index.document(docId).getField(fromField).stringValue();
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    @Override
    public void clear() {
        try {
            this.leftToRightMap.clear();
            this.rightToLeftMap.clear();
        } finally {
            built = false;
        }
    }

    @Override
    public long getMemoryUsage() {
        //TODO
        return 0;
    }

    @Override
    public JoinIndexType getType() {
        return JoinIndexType.IN_MEMORY;
    }

}
