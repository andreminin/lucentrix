package org.apache.lucene.util;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnhancedJoinUtils {
    // Note: JoinIndex interface/classes (InMemoryJoinIndex, OnDiskJoinIndex, JoinIndexConfig)
    // are assumed to exist based on the user prompt.
    private static final Map<String, JoinIndex> joinIndices = new ConcurrentHashMap<>();

    public static BitDocIdSet join(Query leftQuery, IndexSearcher fromSearcher, String leftField,
                                   IndexReader rightIndex, String rightField,
                                   ScoreMode scoreMode,
                                   String indexName) throws IOException {

        // Try to use optimized join index first
        JoinIndex joinIndex = joinIndices.get(indexName);
        if (joinIndex != null && joinIndex.isBuilt()) {
            return performIndexedJoin(fromSearcher.getIndexReader(), leftField, rightIndex, rightField,
                    leftQuery, joinIndex);
        }

        // --- Fallback: Use standard Lucene JoinUtil to create a Query ---

        // JoinUtil creates a Query that runs on the 'to' index (rightIndex).
        // This query implicitly handles the mapping from the 'from' search results to the 'to' index docs.
        Query joinQuery = JoinUtil.createJoinQuery(leftField, false, rightField, leftQuery, fromSearcher, scoreMode);


        // Execute the generated join query against the 'rightIndex'.
        IndexSearcher rightSearcher = new IndexSearcher(rightIndex);

        // We need to collect ALL results into a BitSet.
        // Use a MatchAllDocsQuery within a search if we need all potential join hits,
        // but the joinQuery IS the filter itself.

        // Standard way to collect all matching docs into a BitSet:
        final FixedBitSet resultBitSet = new FixedBitSet(rightIndex.maxDoc());

        rightSearcher.search(joinQuery, new Collector() {
            // No need to score
            @Override
            public org.apache.lucene.search.ScoreMode scoreMode() {
                return org.apache.lucene.search.ScoreMode.COMPLETE_NO_SCORES;
            }

            @Override
            public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
                final int docBase = context.docBase;
                return new LeafCollector() {
                    @Override
                    public void setScorer(Scorable scorer) throws IOException {
                        // Not used as we are just collecting IDs
                    }

                    @Override
                    public void collect(int docId) throws IOException {
                        // Convert segment-specific docId to global docId and set the bit
                        resultBitSet.set(docBase + docId);
                    }
                };
            }
        });

        return new BitDocIdSet(resultBitSet);
    }

    private static BitDocIdSet performIndexedJoin(IndexReader leftIndex, String leftField,
                                                  IndexReader rightIndex, String rightField,
                                                  Query leftQuery,
                                                  JoinIndex joinIndex) throws IOException {

        FixedBitSet result = new FixedBitSet(rightIndex.maxDoc());
        IndexSearcher leftSearcher = new IndexSearcher(leftIndex);

        // Execute left query
        // NOTE: leftIndex.maxDoc() can be very large; this might load too many docs into memory if the query matches a lot
        TopDocs leftDocs = leftSearcher.search(leftQuery, leftIndex.maxDoc());

        for (ScoreDoc leftScoreDoc : leftDocs.scoreDocs) {
            int leftDocId = leftScoreDoc.doc;

            // Use join index to find matching right documents
            // NOTE: joinIndex.getMatchingDocs needs to correctly handle segment translations if indices have merged/changed
            DocIdSet matchingRightDocs = joinIndex.getMatchingDocs(leftIndex, leftDocId, leftField, rightField);

            if (matchingRightDocs != null) {
                DocIdSetIterator rightIt = matchingRightDocs.iterator();
                if (rightIt == null) continue;
                int rightDocId;
                while ((rightDocId = rightIt.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                    result.set(rightDocId);
                }
            }
        }

        return new BitDocIdSet(result);
    }

    public static void createJoinIndex(String indexName, JoinIndexConfig config,
                                       IndexReader leftIndex, String leftField,
                                       IndexReader rightIndex, String rightField) throws IOException {

        JoinIndex joinIndex;
        switch (config.getType()) {
            case IN_MEMORY:
                joinIndex = new InMemoryJoinIndex(leftField, rightField);
                break;
            case ON_DISK:
                joinIndex = new OnDiskJoinIndex(config.getDiskPath(), leftField, rightField);
                break;
            default:
                throw new IllegalArgumentException("Unsupported join index type: " + config.getType());
        }

        joinIndex.build(leftIndex, rightIndex);

        joinIndices.put(indexName, joinIndex);
    }

    public static void removeJoinIndex(String indexName) {
        JoinIndex joinIndex = joinIndices.remove(indexName);
        if (joinIndex != null) {
            joinIndex.clear();
        }
    }
}
