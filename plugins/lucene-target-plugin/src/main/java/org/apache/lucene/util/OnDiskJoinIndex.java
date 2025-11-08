package org.apache.lucene.util;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

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

    public OnDiskJoinIndex(Path indexDir, String leftField, String rightField) {
        this.leftField = leftField;
        this.rightField = rightField;

        this.indexDir = indexDir;
    }

    @Override
    public void build(IndexReader leftIndex, IndexReader rightIndex) throws IOException {
        try {
            IndexWriterConfig config = new IndexWriterConfig(new KeywordAnalyzer());
            FSDirectory directory = MMapDirectory.open(indexDir);
            writer = new IndexWriter(directory, config);

            // Index left side relationships
            indexRelationships(leftIndex, leftField, "left");

            // Index right side relationships
            indexRelationships(rightIndex, rightField, "right");

            writer.commit();
            writer.close();

            // Open for searching
            reader = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
        } finally {
            built = true;
        }
    }

    private void indexRelationships(IndexReader sourceIndex, String field,
                                    String side) throws IOException {
        Terms terms = MultiTerms.getTerms(sourceIndex, field);
        if (terms == null) return;

        TermsEnum termsEnum = terms.iterator();
        BytesRef term;

        while ((term = termsEnum.next()) != null) {
            String termValue = term.utf8ToString();
            PostingsEnum postings = termsEnum.postings(null);

            int docId;
            while ((docId = postings.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                Document joinDoc = new Document();
                joinDoc.add(new StringField("value", termValue, Field.Store.YES));
                joinDoc.add(new StringField("side", side, Field.Store.YES));
                joinDoc.add(new IntField("docId", docId, Field.Store.YES));
                joinDoc.add(new StringField("key", side + "_" + termValue + "_" + docId, Field.Store.YES));

                writer.addDocument(joinDoc);
            }
        }
    }

    @Override
    public DocIdSet getMatchingDocs(IndexReader leftIndex, int docId, String fromField, String toField) throws IOException {
        // Get field value from source document
        String fieldValue = getFieldValue(leftIndex, docId, fromField);
        if (fieldValue == null) return null;

        // Query for matching documents
        String targetSide = fromField.equals(leftField) ? "right" : "left";

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.add(new TermQuery(new Term("value", fieldValue)), BooleanClause.Occur.MUST);
        queryBuilder.add(new TermQuery(new Term("side", targetSide)), BooleanClause.Occur.MUST);

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
        return JoinIndexType.ON_DISK;
    }

}
