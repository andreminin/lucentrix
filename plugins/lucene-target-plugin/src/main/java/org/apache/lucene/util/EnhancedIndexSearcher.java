package org.apache.lucene.util;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.search.join.ScoreMode;

import java.io.IOException;
import java.util.Objects;

public class EnhancedIndexSearcher extends IndexSearcher {

    public EnhancedIndexSearcher(IndexReader r) {
        super(r);
    }

    public TopDocs joinSearch(Query fromQuery,
                              String fromField,
                              String toField,
                              String joinIndexName,
                              Sort sort, int n) throws IOException {

        // Note: The 'toIndex' here will be the IndexReader associated with 'this' searcher's IndexReader
        // if you want the final search results to appear in the current search context.
        // Assuming the 'toIndex' passed in corresponds to the reader of this EnhancedIndexSearcher:

        BitDocIdSet joinResult = EnhancedJoinUtils.join(fromQuery, this, fromField,
                this.getIndexReader(), toField, ScoreMode.Total, joinIndexName
        );

        // We use the standard searchAfter method with our custom filtering query
        return searchAfter(null, createJoinQuery(joinResult), n, sort);
    }

    /**
     * Converts a transient, segment-specific DocIdSet into a Lucene Query.
     *
     * NOTE: This Query is only valid for the specific IndexReader that generated the joinResult.
     */
    private Query createJoinQuery(DocIdSet joinResult) {
        if (joinResult == null) {
            return new MatchNoDocsQuery("No join matches found");
        }
        return new DocIdSetFilterQuery(joinResult);
    }

    // --- Inner Class for the custom Query ---

    /**
     * A Query that wraps a single, specific DocIdSet.
     * This is an advanced/internal utility class and relies on the DocIdSet
     * being compatible with the IndexReader used to run the search.
     */
    private static class DocIdSetFilterQuery extends Query {
        private final DocIdSet docIdSet;

        public DocIdSetFilterQuery(DocIdSet docIdSet) {
            this.docIdSet = Objects.requireNonNull(docIdSet);
        }

        @Override
        public Weight createWeight(IndexSearcher searcher, org.apache.lucene.search.ScoreMode scoreMode, float boost) {
            // This filter does not score, it only matches documents.
            // We use a ConstantScoreWeight to apply the boost without complex scoring logic.
            return new ConstantScoreWeight(this, boost) {

                public Bits getLiveDocs(LeafReaderContext context) {
                    // We don't track live docs explicitly here, relying on the underlying reader
                    return context.reader().getLiveDocs();
                }

                @Override
                public Scorer scorer(LeafReaderContext context) throws IOException {
                    // In a real-world scenario, you MUST ensure this DocIdSet is specific
                    // to THIS EXACT segment (context.ord/context.reader().toString() etc).
                    // As implemented in EnhancedJoinUtils, it returns a BitDocIdSet for the *entire*
                    // IndexReader, which means we need to translate global IDs to segment-specific IDs.

                    // Since EnhancedJoinUtils returns a Flat BitSet for the whole index (based on the impl),
                    // we must handle the segment offsets:

                    DocIdSetIterator it = docIdSet.iterator();
                    if (it == null) {
                        return null;
                    }

                    // Wrap the iterator to handle global-to-segment translation
                    DocIdSetIterator segmentIt = new GlobalDocIdSetIterator(it, context);

                    // If we only filter, a null scorer is fine (handled by ConstantScoreWeight logic)
                    // We need to return a ConstantScoreScorer that uses the segmentIt.
                    return new ConstantScoreScorer(this, score(), scoreMode, segmentIt);
                }

                @Override
                public boolean isCacheable(LeafReaderContext ctx) {
                    return false; // DocIdSet is transient/in-memory, not cacheable generally
                }
            };
        }

        @Override
        public String toString(String field) {
            return "DocIdSetFilterQuery(...)";
        }

        @Override
        public void visit(QueryVisitor queryVisitor) {

        }

        // Boilerplate equals/hashCode for Query
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DocIdSetFilterQuery that = (DocIdSetFilterQuery) o;
            // Equality check might be complex for arbitrary DocIdSets, assuming reference equality here or value equality if possible
            return Objects.equals(docIdSet, that.docIdSet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(docIdSet);
        }
    }

    // --- Helper class to translate global DocIDs to segment DocIDs ---

    /**
     * Helper Iterator that takes a global DocIdSetIterator and translates
     * the IDs to be relative to a specific segment context.
     * This is necessary because EnhancedJoinUtils creates a flat result set for the entire index.
     */

    private static class GlobalDocIdSetIterator extends DocIdSetIterator {
        private final DocIdSetIterator globalIterator;
        private final int docBase;
        private final int maxDocInSegment;
        private int currentSegmentDocId = -1;

        GlobalDocIdSetIterator(DocIdSetIterator globalIterator, LeafReaderContext context) {
            this.globalIterator = globalIterator;
            this.docBase = context.docBase;
            this.maxDocInSegment = context.reader().maxDoc();
        }

        @Override
        public int nextDoc() throws IOException {
            int globalDocId;
            do {
                globalDocId = globalIterator.nextDoc();
            } while (globalDocId != NO_MORE_DOCS && (globalDocId < docBase || globalDocId >= docBase + maxDocInSegment));

            if (globalDocId == NO_MORE_DOCS) {
                return currentSegmentDocId = NO_MORE_DOCS;
            }

            // Теперь globalDocId находится в пределах сегмента: [docBase, docBase+maxDocInSegment)
            return currentSegmentDocId = globalDocId - docBase;
        }

        @Override
        public int advance(int target) throws IOException {
            // target - это сегментный docId, поэтому преобразуем в глобальный
            int globalTarget = target + docBase;
            int globalDocId = globalIterator.advance(globalTarget);
            while (globalDocId != NO_MORE_DOCS && (globalDocId < docBase || globalDocId >= docBase + maxDocInSegment)) {
                globalDocId = globalIterator.nextDoc();
            }

            if (globalDocId == NO_MORE_DOCS) {
                return currentSegmentDocId = NO_MORE_DOCS;
            }

            return currentSegmentDocId = globalDocId - docBase;
        }

        @Override
        public int docID() {
            return currentSegmentDocId;
        }

        @Override
        public long cost() {
            return globalIterator.cost();
        }
    }


}
