


Lucene IndexWriter to index documents with join fields (join_key, category),

An embedded H2 in-memory database with a table holding (doc_id, join_key, category) and proper B-tree indexes,

Use of H2’s internal B-tree index scan (PageDataIndex and PageIndexCursor) to efficiently fetch matching doc IDs for join keys,

A simple Lucene custom join utility method that queries H2 internal index directly to produce matching doc IDs,

A demonstration of searching Lucene documents filtered by join keys using that H2-backed join.