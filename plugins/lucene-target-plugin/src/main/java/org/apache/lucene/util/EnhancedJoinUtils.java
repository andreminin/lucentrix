// EnhancedJoinUtils.java - minor cleanup
package org.apache.lucene.util;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class EnhancedJoinUtils {
    // Note: JoinIndex interface/classes (InMemoryJoinIndex, OnDiskJoinIndex, JoinIndexConfig)
    // are assumed to exist based on the user prompt.
    private static final Map<String, JoinIndex> joinIndices = new ConcurrentHashMap<>();
    private static final ReentrantLock memoryLock = new ReentrantLock();
    private static long totalMemoryUsage = 0;
    private static final long MEMORY_THRESHOLD = Runtime.getRuntime().maxMemory() / 4; // 25% of heap

    private static final ScheduledExecutorService cleanupExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "JoinIndex-Cleanup");
                t.setDaemon(true);
                return t;
            });

    static {
        // Schedule periodic cleanup
        cleanupExecutor.scheduleAtFixedRate(EnhancedJoinUtils::cleanupExpiredIndices,
                1, 1, TimeUnit.MINUTES);
    }

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

        // Fallback to standard Lucene JoinUtil
        Query joinQuery = JoinUtil.createJoinQuery(leftField, false, rightField,
                leftQuery, fromSearcher, scoreMode);

        IndexSearcher rightSearcher = new IndexSearcher(rightIndex);
        final FixedBitSet resultBitSet = new FixedBitSet(rightIndex.maxDoc());

        rightSearcher.search(joinQuery, new Collector() {
            @Override
            public org.apache.lucene.search.ScoreMode scoreMode() {
                return org.apache.lucene.search.ScoreMode.COMPLETE_NO_SCORES;
            }

            @Override
            public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
                final int docBase = context.docBase;
                return new LeafCollector() {
                    @Override
                    public void setScorer(Scorable scorer) throws IOException {}

                    @Override
                    public void collect(int docId) throws IOException {
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

        // Use collector for better performance with large result sets
        leftSearcher.search(leftQuery, new Collector() {
            @Override
            public org.apache.lucene.search.ScoreMode scoreMode() {
                return org.apache.lucene.search.ScoreMode.COMPLETE_NO_SCORES;
            }

            @Override
            public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
                return new LeafCollector() {
                    @Override
                    public void setScorer(Scorable scorer) throws IOException {}

                    @Override
                    public void collect(int docId) throws IOException {
                        int globalDocId = context.docBase + docId;
                        DocIdSet matchingRightDocs = joinIndex.getMatchingDocs(
                                leftIndex, globalDocId, leftField, rightField);

                        if (matchingRightDocs != null) {
                            DocIdSetIterator rightIt = matchingRightDocs.iterator();
                            if (rightIt != null) {
                                int rightDocId;
                                while ((rightDocId = rightIt.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                                    result.set(rightDocId);
                                }
                            }
                        }
                    }
                };
            }
        });

        return new BitDocIdSet(result);
    }

    public static void createJoinIndex(String indexName, JoinIndexConfig config,
                                       IndexReader leftIndex, String leftField,
                                       IndexReader rightIndex, String rightField) throws IOException {

        // Check memory constraints before creating new index
        enforceMemoryConstraints();

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

        memoryLock.lock();
        try {
            joinIndices.put(indexName, joinIndex);
            totalMemoryUsage += joinIndex.getMemoryUsage();
        } finally {
            memoryLock.unlock();
        }
    }

    public static void removeJoinIndex(String indexName) {
        memoryLock.lock();
        try {
            JoinIndex joinIndex = joinIndices.remove(indexName);
            if (joinIndex != null) {
                totalMemoryUsage -= joinIndex.getMemoryUsage();
                joinIndex.clear();
            }
        } finally {
            memoryLock.unlock();
        }
    }

    private static void enforceMemoryConstraints() {
        if (totalMemoryUsage > MEMORY_THRESHOLD) {
            cleanupLRUIndices();
        }
    }

    private static void cleanupExpiredIndices() {
        long now = System.currentTimeMillis();
        memoryLock.lock();
        try {
            joinIndices.entrySet().removeIf(entry -> {
                JoinIndex index = entry.getValue();
                if (index instanceof InMemoryJoinIndex) {
                    InMemoryJoinIndex memIndex = (InMemoryJoinIndex) index;
                    if (now - memIndex.getLastAccessTime() >
                            getConfigForIndex(entry.getKey()).getTimeToLiveMs()) {
                        totalMemoryUsage -= index.getMemoryUsage();
                        index.clear();
                        return true;
                    }
                }
                return false;
            });
        } finally {
            memoryLock.unlock();
        }
    }

    private static void cleanupLRUIndices() {
        memoryLock.lock();
        try {
            joinIndices.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof InMemoryJoinIndex)
                    .sorted((e1, e2) -> Long.compare(
                            ((InMemoryJoinIndex) e2.getValue()).getLastAccessTime(),
                            ((InMemoryJoinIndex) e1.getValue()).getLastAccessTime()))
                    .skip(joinIndices.size() / 2) // Remove oldest half
                    .forEach(entry -> {
                        totalMemoryUsage -= entry.getValue().getMemoryUsage();
                        entry.getValue().clear();
                        joinIndices.remove(entry.getKey());
                    });
        } finally {
            memoryLock.unlock();
        }
    }

    private static JoinIndexConfig getConfigForIndex(String indexName) {
        // In real implementation, you'd store configs separately
        return JoinIndexConfig.builder()
                .indexName(indexName)
                .type(JoinIndexType.IN_MEMORY)
                .timeToLiveMs(300_000)
                .build();
    }

    public static void shutdown() {
        cleanupExecutor.shutdown();
        joinIndices.values().forEach(JoinIndex::clear);
        joinIndices.clear();
        totalMemoryUsage = 0;
    }
}