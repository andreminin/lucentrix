// OnDiskJoinIndex.java - fixed version handling
package org.apache.lucene.util;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Path;

public class OnDiskJoinIndex implements JoinIndex {
    private final Path indexDir;
    private IndexWriter writer;
    private IndexSearcher searcher;
    private IndexReader reader;
    private final String leftField;
    private final String rightField;
    private boolean built = false;
    private long indexVersion = -1;

    public OnDiskJoinIndex(Path indexDir, String leftField, String rightField) {
        this.indexDir = indexDir;
        this.leftField = leftField;
        this.rightField = rightField;
    }

    @Override
    public void build(IndexReader leftIndex, IndexReader rightIndex) throws IOException {
        long newVersion = calculateIndexVersion(leftIndex, rightIndex);
        if (this.indexVersion == newVersion && built) {
            return; // Already up-to-date
        }

        this.indexVersion = newVersion;

        try (Directory directory = FSDirectory.open(indexDir)) {
            IndexWriterConfig config = new IndexWriterConfig(new KeywordAnalyzer());
            writer = new IndexWriter(directory, config);

            // Index relationships using DocValues for efficiency
            indexRelationshipsWithDocValues(leftIndex, leftField, "left");
            indexRelationshipsWithDocValues(rightIndex, rightField, "right");

            writer.commit();
            writer.close();

            // Open for searching
            reader = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
            built = true;
        }
    }

    private void indexRelationshipsWithDocValues(IndexReader sourceIndex, String field,
                                                 String side) throws IOException {
        for (LeafReaderContext context : sourceIndex.leaves()) {
            LeafReader reader = context.reader();
            SortedDocValues docValues = reader.getSortedDocValues(field);
            if (docValues == null) continue;

            int docBase = context.docBase;
            for (int docId = 0; docId < reader.maxDoc(); docId++) {
                if (docValues.advanceExact(docId)) {
                    BytesRef termValue = docValues.lookupOrd(0);
                    int globalDocId = docBase + docId;

                    Document joinDoc = new Document();
                    joinDoc.add(new StringField("value", termValue.utf8ToString(), Field.Store.YES));
                    joinDoc.add(new StringField("side", side, Field.Store.YES));
                    joinDoc.add(new IntField("docId", globalDocId, Field.Store.YES));
                    joinDoc.add(new StringField("key",
                            side + "_" + termValue.utf8ToString() + "_" + globalDocId, Field.Store.YES));

                    writer.addDocument(joinDoc);
                }
            }
        }
    }

    private long calculateIndexVersion(IndexReader left, IndexReader right) {
        return getVersion(left) + getVersion(right);
    }

    private long getVersion(IndexReader r) {
        if (r instanceof DirectoryReader dr) {
            return dr.getVersion();        // ✔ Only place where version exists
        }
        // No version available for non-DirectoryReader
        return 0L;
    }

    @Override
    public DocIdSet getMatchingDocs(IndexReader leftIndex, int docId, String fromField, String toField) throws IOException {
        if (!built) {
            throw new IllegalStateException("Index not built");
        }

        // Get field value using DocValues
        BytesRef fieldValue = getFieldValueWithDocValues(leftIndex, docId, fromField);
        if (fieldValue == null) return null;

        // Query for matching documents
        String targetSide = fromField.equals(leftField) ? "right" : "left";

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.add(new TermQuery(new Term("value", fieldValue.utf8ToString())),
                BooleanClause.Occur.MUST);
        queryBuilder.add(new TermQuery(new Term("side", targetSide)),
                BooleanClause.Occur.MUST);

        TopDocs topDocs = searcher.search(queryBuilder.build(), Integer.MAX_VALUE);

        if (topDocs.scoreDocs.length == 0) {
            return null;
        }

        FixedBitSet bitSet = new FixedBitSet(reader.maxDoc());
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            int targetDocId = doc.getField("docId").numericValue().intValue();
            bitSet.set(targetDocId);
        }

        return new BitDocIdSet(bitSet);
    }

    private BytesRef getFieldValueWithDocValues(IndexReader index, int docId, String field) throws IOException {
        for (LeafReaderContext context : index.leaves()) {
            if (docId >= context.docBase && docId < context.docBase + context.reader().maxDoc()) {
                int segmentDocId = docId - context.docBase;
                LeafReader reader = context.reader();
                SortedDocValues docValues = reader.getSortedDocValues(field);

                if (docValues != null && docValues.advanceExact(segmentDocId)) {
                    return docValues.lookupOrd(0);
                }
                break;
            }
        }
        return null;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    @Override
    public void clear() {
        try {
            if (reader != null) {
                reader.close();
            }
            built = false;
        } catch (IOException e) {
            // Log warning
        }
    }

    @Override
    public long getMemoryUsage() {
        return 0; // Disk-based, minimal memory usage
    }

    @Override
    public JoinIndexType getType() {
        return JoinIndexType.ON_DISK;
    }
}