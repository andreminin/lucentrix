package org.lucentrix.metaframe.plugin.solr;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.lucentrix.metaframe.plugin.solr.SolrFieldType.NOT_EXISTING;

public class SolrIndexClient {
    private static final Logger logger = LoggerFactory.getLogger(SolrIndexClient.class);

    private static final int indexUpdateDocMaxCount = 500;

    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private SolrClient solrClient;
    protected final SolrConfig solrConfig;
    protected final String collection;
    protected final AutoCommitPolicy autoCommitPolicy;
    protected final SolrCommitOptions commitOptions;
    protected final String docIdIndexField;
    private final SolrIndexSchema indexSchema;
    private final SolrFieldMapper mapper;


    public SolrIndexClient(SolrClient solrClient, String collection, SolrFieldMapper mapper) {
        this(solrClient, collection, new SolrIndexSchema(), mapper);
    }

    public SolrIndexClient(SolrClient solrClient, SolrFieldMapper mapper) {
        this(solrClient, null, new SolrIndexSchema(), mapper);
    }

    public SolrIndexClient(SolrClient solrClient, SolrIndexSchema schema, SolrFieldMapper mapper) {
        this(solrClient, null, schema, mapper);
    }

    public SolrIndexClient(SolrConfig solrConfig, SolrFieldMapper mapper) {
        this(solrConfig, new SolrIndexSchema(), mapper);
    }

    public SolrIndexClient(SolrConfig solrConfig, SolrIndexSchema indexSchema, SolrFieldMapper mapper) {
        Objects.requireNonNull(solrConfig);
        Objects.requireNonNull(indexSchema);

        this.solrConfig = solrConfig;
        this.collection = this.solrConfig.getCollection();
        this.indexSchema = indexSchema;
        this.autoCommitPolicy = this.solrConfig.getAutoCommitPolicy() == null ? AutoCommitPolicy.DEFAULT : solrConfig.getAutoCommitPolicy();
        this.docIdIndexField = indexSchema.getIdField();
        this.commitOptions = solrConfig.getSolrCommitOptions();
        this.mapper = mapper;
    }

    public SolrIndexClient(SolrClient solrClient, String collection, SolrIndexSchema indexSchema, SolrFieldMapper mapper) {
        this.solrClient = solrClient;
        this.solrConfig = null;
        this.collection = collection;
        this.indexSchema = indexSchema;
        this.autoCommitPolicy = AutoCommitPolicy.DEFAULT;
        this.commitOptions = new SolrCommitOptions();
        this.docIdIndexField = indexSchema.getIdField();
        this.mapper = mapper;
    }

    public void open() {
        rwl.readLock().lock();
        try {
            if (solrClient != null && getIndexSchema().isInitialized()) {
                return;
            }
        } finally {
            rwl.readLock().unlock();
        }

        rwl.writeLock().lock();
        try {
            if (solrClient == null) {
                if (solrConfig == null) {
                    throw new IllegalStateException("Solr connection configuration is null");
                }
                solrClient = SolrUtils.createCloudHttp2SolrClient(solrConfig);
            }

            if (!getIndexSchema().isInitialized()) {
                try {
                    loadSchema();
                } catch (Exception error) {
                    throw new RuntimeException("Error loading SOLR schema", error);
                }
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }


    public void rollback() {
        rwl.readLock().lock();
        try {
            assertClientNotNull();

            try {
                UpdateRequest updateRequest = new UpdateRequest();
                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    updateRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                updateRequest.rollback();

                UpdateResponse response = updateRequest.process(solrClient, collection);
                // check status, throw error if status is invalid
                int status = response.getStatus();
                logger.debug("Response status code={}, QTime: {}", status, response.getQTime());

                if (status != 0) {
                    throw new RuntimeException("SOLR response status code is not equal 0: " + status);
                }

                logger.debug("Rollback successful: " + response.toString());
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }
        } finally {
            rwl.readLock().unlock();
        }
    }

    public QueryResponse process(JsonQueryRequest jsonQueryRequest) throws SolrServerException, IOException {
        open();

        return jsonQueryRequest.process(solrClient, collection);
    }

    public QueryResponse process(QueryRequest queryRequest) throws SolrServerException, IOException {
        open();

        return queryRequest.process(solrClient, collection);
    }

    public SolrIndexSchema getIndexSchema() {
        return indexSchema;
    }


    public boolean isClosed() {
        rwl.readLock().lock();
        try {
            return solrClient == null;
        } finally {
            rwl.readLock().unlock();
        }

    }


    public SolrFieldType getFieldType(Field<?> field) {
        open();

        rwl.readLock().lock();
        try {
            assertClientNotNull();

            String solrField = SolrFieldSerde.INSTANCE.toString(field);

            return getTypeFromSchema(solrField);
        } finally {
            rwl.readLock().unlock();
        }
    }

    protected SolrFieldType getTypeFromSchema(String fieldName) {
        SolrFieldType indexFieldType = indexSchema.getFieldType(fieldName);
        if (indexFieldType == null || indexFieldType.getType() == null || NOT_EXISTING.equals(indexFieldType)) {
            throw new RuntimeException("Index field type is not found in schema: " + fieldName);
        }
        return indexFieldType;
    }


    public List<Field<?>> getFieldNames() {
        rwl.readLock().lock();
        try {
            assertClientNotNull();

            try {
                LukeRequest lukeRequest = new LukeRequest();
                lukeRequest.setNumTerms(0);

                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    lukeRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                LukeResponse lukeResponse = lukeRequest.process(solrClient, collection);

                Map<String, LukeResponse.FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

                List<Field<?>> fieldNames = new ArrayList<>();

                for (Map.Entry<String, LukeResponse.FieldInfo> entry : fieldInfoMap.entrySet()) {
                    String fieldName = entry.getKey();

                    fieldNames.add(SolrFieldSerde.INSTANCE.fromString(fieldName));
                }

                return fieldNames;
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }
        } finally {
            rwl.readLock().unlock();
        }
    }

    public SearchResult search(String query, int start, int count, String sessionId) {
        rwl.readLock().lock();
        try {
            assertClientNotNull();

            assert count >= 0;
            assert start >= 0;

            JsonQueryRequest jsonQueryRequest = new JsonQueryRequest();
            jsonQueryRequest.setQuery(query);
            jsonQueryRequest.setLimit(count);
            jsonQueryRequest.setOffset(start);

            SearchResult.SearchResultBuilder searchResult = SearchResult.builder();

            List<LxDocument> documents = new ArrayList<>();

            try {
                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    jsonQueryRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                QueryResponse response = jsonQueryRequest.process(solrClient, collection);

                if (response.getStatus() != 0) {
                    logger.warn("SOLR response is not OK: {}", response);

                    return searchResult.build();
                }

                SolrDocumentList documentList = response.getResults();
                int numFound = (int) documentList.getNumFound();
                int numReturned = documentList.size();

                logger.debug("Found total {} documents in SOLR index, returned {} documents", numFound, numReturned);

                for (int i = 0; i < numReturned; i++) {
                    documents.add(SolrUtils.toLxDocument(documentList.get(i)));
                }

                searchResult.totalCount(numFound);
                if (count > 0) {
                    searchResult.numberOfPages((count - 1 + numFound) / count);
                }
                searchResult.documents(documents);

                return searchResult.build();
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }
        } finally {
            rwl.readLock().unlock();
        }
    }


    public void optimize(int segments) {
        if (solrClient == null) {
            open();
        }

        rwl.writeLock().lock();
        try {
            assertClientNotNull();
            solrClient.optimize(collection, true, true, segments);
        } catch (SolrServerException ex) {
            throw new RuntimeException("Solr server error", ex);
        } catch (Exception e) {
            throw new RuntimeException("Solr client error", e);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public void commit() {
        commit(commitOptions);
    }


    public void commit(SolrCommitOptions options) {
        if (solrConfig != null && solrConfig.getAutoCommitPolicy().isEnabled()) {
            return;
        }

        if (rwl.readLock().tryLock()) {
            try {
                if (solrClient == null) {
                    logger.debug("Solr index delete documents request ignored: solrClient client is not initialized");
                    return;
                }

                try {
                    logger.debug("Starting solrClient index commit");
                    UpdateRequest updateRequest = new UpdateRequest();
                    if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                        updateRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                    }

                    if (options != null) {
                        updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, options.isWaitForFlush(), options.isWaitForSearcher(), options.isSoftCommit());
                    } else {
                        updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                    }

                    UpdateResponse response = updateRequest.process(solrClient, collection);
                    // check status, throw error if status is invalid
                    int status = response.getStatus();
                    logger.info("SOLR commit response status code={}, QTime: {}", status, response.getQTime());

                    if (status != 0) {
                        throw new RuntimeException("SOLR response status code is not equal 0: " + status);
                    }
                    logger.debug("Solr index commit completed");
                } catch (SolrServerException ex) {
                    throw new RuntimeException("Solr server error", ex);
                } catch (Exception e) {
                    throw new RuntimeException("Solr client error", e);
                }
            } finally {
                rwl.readLock().unlock();
            }
        }
    }

    public boolean isSchemaLoaded() {
        return getIndexSchema().isInitialized();
    }

    private void assertClientNotNull() {
        if (solrClient == null) {
            throw new RuntimeException("SOLR client is closed");
        }
    }

    protected void loadSchema() {
        SolrIndexSchema indexSchema = getIndexSchema();
        if (solrClient != null && indexSchema != null) {
            indexSchema.init(solrClient, collection,
                    solrConfig == null ? null : solrConfig.getUser(),
                    solrConfig == null ? null : solrConfig.getPassword());
        }
    }

    private void sleep(int delay) {
        if (delay > 0) {
            logger.info("SOLR index client - sleep for {} msec", delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        rwl.readLock().lock();
        try {
            assertClientNotNull();

            logger.debug("Deleting documents from solrClient index by unique key ($id@s) doc ids: {}", ids);

            for (List<String> chunk : Lists.partition(ids, indexUpdateDocMaxCount)) {
                try {
                    for (String id : chunk) {
                        if (StringUtils.isBlank(id)) {
                            throw new RuntimeException("Deleted document ids contains null value: " + chunk);
                        }
                    }
                    UpdateRequest deleteRequest = new UpdateRequest();
                    deleteRequest.deleteById(chunk);
                    deleteRequest.setCommitWithin(-1);

                    if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                        deleteRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                    }

                    UpdateResponse response = deleteRequest.process(solrClient, collection);

                    // check status, throw error if status is invalid
                    int status = response.getStatus();
                    logger.info("Delete request ids={} status code={}, QTime: {}", ids, status, response.getQTime());

                    if (status != 0) {
                        throw new RuntimeException("SOLR response status code is not equal 0: " + status);
                    }
                } catch (SolrServerException ex) {
                    throw new RuntimeException("Solr server error", ex);
                } catch (Exception e) {
                    throw new RuntimeException("Solr client error", e);
                }
            }
        } finally {
            rwl.readLock().unlock();
        }
    }


    public void updateIndex(List<LxEvent> events, SolrCommitOptions options) {
        if (events == null || events.isEmpty()) {
            logger.debug("Solr index update request ignored: input documents list is empty");
            return;
        }

        List<String> deletedIds = null;
        Map<String, LxDocument> mergedDocuments = new HashMap<>();
        Map<String, LxDocument> replacedDocuments = new HashMap<>();

        for (LxEvent event : events) {
            if (event == null) {
                continue;
            }
            if (event.getDocument() == null) {
                continue;
            }

            LxDocument document = event.getDocument();
            String id = document.getId();

            switch (event.getAction()) {
                case DELETE -> {
                    if (deletedIds == null) {
                        deletedIds = new ArrayList<>();
                    }
                    deletedIds.add(id);
                    mergedDocuments.remove(id);
                    replacedDocuments.remove(id);
                }
                case MERGE -> {
                    LxDocument merged = mergedDocuments.get(id);

                    if (merged == null) {
                        merged = replacedDocuments.remove(id);
                    }
                    if (merged != null) {
                        merged = merged.toBuilder().fields(event.getDocument()).build();
                    } else {
                        merged = event.getDocument();
                    }
                    mergedDocuments.put(id, merged);
                    replacedDocuments.remove(id);
                }
                case REPLACE -> {
                    mergedDocuments.remove(id);
                    replacedDocuments.put(id, event.getDocument());
                }
            }
        }

        logger.info("Document ids: {}; updated document ids: {}; deleted document ids: {}",
                replacedDocuments.keySet(), mergedDocuments.keySet(), deletedIds);

        deleteByIds(deletedIds);

        List<SolrInputDocument> solrInputDocuments = new ArrayList<>();

        for (LxDocument document : replacedDocuments.values()) {
            SolrInputDocument solrInputDocument = SolrUtils.toSolrInputDocument(document, getIndexSchema(), mapper);
            if (solrInputDocument != null) {
                solrInputDocuments.add(solrInputDocument);
            }
        }

        if (!mergedDocuments.isEmpty()) {
            long start = System.currentTimeMillis();
            for (List<String> docIds : Lists.partition(new ArrayList<>(mergedDocuments.keySet()), indexUpdateDocMaxCount)) {
                List<LxDocument> existingDocs = searchDocumentById(docIds.toArray(new String[0]));

                Collection<LxDocument> documentUpdates = mergedDocuments.values();

                logger.debug("Merging new documents = {} with existing documents={}", documentUpdates, existingDocs);

                documentUpdates = mergeDocuments(existingDocs, documentUpdates);

                logger.debug("Merged documents: {}", documentUpdates);

                for (LxDocument documentUpdate : documentUpdates) {
                    SolrInputDocument solrInputDocument = SolrUtils.toSolrInputDocument(documentUpdate, getIndexSchema(), mapper);
                    if (solrInputDocument != null) {
                        solrInputDocuments.add(solrInputDocument);
                    }
                }
            }
            long measurement = System.currentTimeMillis() - start;
            logger.info("SOLR index merged {} documents in {} msec", mergedDocuments.size(), measurement);
        }

        if (solrInputDocuments.isEmpty()) {
            logger.debug("Solr index replace documents list is empty");
            return;
        }

        int uploadedCount = 0;
        long start = System.currentTimeMillis();
        for (List<SolrInputDocument> solrDocuments : Lists.partition(solrInputDocuments, indexUpdateDocMaxCount)) {
            try {
                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.add(solrDocuments);

                if (options != null) {
                    updateRequest.setWaitSearcher(options.isWaitForSearcher());
                    updateRequest.setCommitWithin(options.getCommitWithin());
                } else {
                    updateRequest.setCommitWithin(-1);
                }

                uploadedCount += solrDocuments.size();

                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    updateRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                rwl.readLock().lock();
                try {
                    assertClientNotNull();
                    UpdateResponse response = updateRequest.process(solrClient, collection);
                    // check status, throw error if status is invalid
                    int status = response.getStatus();
                    logger.info("SOLR update response status code={}, QTime: {}", status, response.getQTime());
                    //logger.warn("{} uploaded to SOLR", solrDocuments.size());
                    if (status != 0) {
                        throw new RuntimeException("SOLR response status code is not equal 0: " + status);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }

            // workaround to reduce queue in solr to avoid cluster sync timeout later
            if (uploadedCount >= 100000) {
                try {
                    logger.debug("Uploaded more than 100K documents, forcing SOLR commit");
                    commit();
                    logger.debug("SOLR index committed");
                } finally {
                    uploadedCount = 0;
                }
            }
        }
        long measurement = System.currentTimeMillis() - start;
        logger.info("SOLR index uploaded {} documents in {} msec", solrInputDocuments.size(), measurement);
    }

    protected Collection<LxDocument> mergeDocuments(List<LxDocument> existingDocuments, Collection<LxDocument> documentUpdates) {
        if (existingDocuments == null || existingDocuments.isEmpty() || documentUpdates == null || documentUpdates.isEmpty()) {
            return documentUpdates;
        }

        Map<String, LxDocument> existingDocMap = new HashMap();
        for (LxDocument existingDoc : existingDocuments) {
            existingDocMap.put(existingDoc.getId(), existingDoc);
        }

        List<LxDocument> mergedDocuments = new ArrayList<>();
        for (LxDocument documentUpdate : documentUpdates) {
            LxDocument.LxDocumentBuilder<?, ?> builder = documentUpdate.toBuilder();

            LxDocument existingDoc = existingDocMap.get(documentUpdate.getId());

            if (existingDoc != null) {
                Set<? extends Field<?>> updatedFieldNames = documentUpdate.fields().keySet();

                for (Field<?> field : existingDoc.fields().keySet()) {
                    if ("_version_".equalsIgnoreCase(field.getName())) {
                        continue;
                    }
                    if (!updatedFieldNames.contains(field)) {
                        builder.field(field, existingDoc.get(field));
                    }
                }
            }
            mergedDocuments.add(builder.build());
        }

        return mergedDocuments;
    }

    public List<LxDocument> searchDocumentById(String... ids) {
        SolrDocumentList solrDocuments;

        rwl.readLock().lock();
        try {
            assertClientNotNull();

            if (ids == null || ids.length == 0) {
                throw new IllegalArgumentException("Must provide an identifier of a document to retrieve.");
            }

            try {
                /*
                ModifiableSolrParams reqParams = new ModifiableSolrParams();
                if (org.apache.solr.common.StringUtils.isEmpty(reqParams.get(CommonParams.QT))) {
                    reqParams.set(CommonParams.QT, "/get");
                }
                reqParams.set("ids", ids);

                JsonQueryRequest jsonQueryRequest = new JsonQueryRequest(reqParams);
                */
                JsonQueryRequest jsonQueryRequest = new JsonQueryRequest()
                        .setQuery("*:*") // Required placeholder query
                        .withFilter("{!terms f=id}" + String.join(",", ids)); // Filter on IDs

                jsonQueryRequest.setLimit(ids.length);

                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    jsonQueryRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                QueryResponse queryResponse = jsonQueryRequest.process(solrClient, collection);

                solrDocuments = queryResponse.getResults();
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }
        } finally {
            rwl.readLock().unlock();
        }

        List<LxDocument> outputDocuments = new ArrayList<>();

        if (solrDocuments != null) {
            for (SolrDocument solrDocument : solrDocuments) {
                LxDocument outputDocument = SolrUtils.toLxDocument(solrDocument);
                outputDocuments.add(outputDocument);
            }
        }

        return outputDocuments;
    }

    public void close() throws IOException {
        rwl.writeLock().lock();
        try {
            if (solrClient != null) {
                try {
                    solrClient.close();
                } finally {
                    solrClient = null;
                }
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    //Used in integration tests
    public void add(List<SolrInputDocument> inputDocuments) {
        rwl.readLock().lock();
        try {
            assertClientNotNull();

            try {
                UpdateRequest updateRequest = new UpdateRequest();
                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    updateRequest.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                updateRequest.add(inputDocuments);
                updateRequest.setCommitWithin(-1);
                UpdateResponse updateResponse = updateRequest.process(solrClient, collection);

                // check status, throw error if status is invalid
                int status = updateResponse.getStatus();
                logger.debug("Response status code={}, QTime: {}", status, updateResponse.getQTime());

                if (status != 0) {
                    throw new RuntimeException("SOLR response status code is not equal 0: " + status);
                }
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }
        } finally {
            rwl.readLock().unlock();
        }
    }

    public Set<String> getFieldValues(Field<?> field, int limit) {
        rwl.readLock().lock();
        try {
            String term = SolrFieldSerde.INSTANCE.toString(field);

            if (limit <= 0) {
                limit = 100;
            }

            LinkedHashSet<String> values = new LinkedHashSet<>();

            SolrQuery query = new SolrQuery();
            query.setRequestHandler("/terms");
            query.setTerms(true);
            query.setTermsLimit(limit);
            //query.setTermsLower("s");
            //query.setTermsPrefix("s");
            query.addTermsField(term);
            query.setTermsMinCount(1);


            try {
                QueryRequest request = new QueryRequest(query);

                if (solrConfig != null && StringUtils.isNotBlank(solrConfig.getUser())) {
                    request.setBasicAuthCredentials(solrConfig.getUser(), solrConfig.getPassword());
                }

                TermsResponse response = request.process(solrClient, collection).getTermsResponse();

                List<TermsResponse.Term> termResp = response.getTerms(term);

                if (termResp != null) {
                    for (TermsResponse.Term termItem : termResp) {
                        values.add(termItem.getTerm());
                    }
                }
            } catch (SolrServerException ex) {
                throw new RuntimeException("Solr server error", ex);
            } catch (Exception e) {
                throw new RuntimeException("Solr client error", e);
            }

            return values;
        } finally {
            rwl.readLock().unlock();
        }
    }

}
