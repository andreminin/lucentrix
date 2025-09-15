package org.lucentrix.metaframe.reactive.crawler.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.lucentrix.metaframe.reactive.crawler.config.SolrConfig;
import org.lucentrix.metaframe.reactive.crawler.model.PdfDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ReactiveSolrService {

    private final Http2SolrClient solrClient;
    private final String collection;

    @Autowired
    public ReactiveSolrService(SolrConfig solrConfig) {
        this.solrClient = solrConfig.solrClient();
        this.collection = solrConfig.getSolrCollection();
    }

    /**
     * Index document reactively. Uses Mono.fromFuture(...) and schedules the blocking SolrJ calls
     * on Reactor's boundedElastic scheduler via CompletableFuture.supplyAsync.
     */
    public Mono<String> indexDocument(PdfDocument pdfDocument) {
        return Mono.fromFuture(indexDocumentAsync(pdfDocument))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    if (response.getStatus() == 0) {
                        return "Document indexed successfully: " + pdfDocument.getFilename();
                    } else {
                        throw new RuntimeException("Failed to index document: " + response);
                    }
                })
                .doOnSuccess(result -> log.info("Indexing successful: {}", pdfDocument.getFilename()))
                .doOnError(error -> log.error("Indexing failed for {}: {}",
                        pdfDocument.getFilename(), error.getMessage()));
    }

    @AllArgsConstructor
    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    static class RequestResult<T> {
        T data;
        Exception error;
    }

    /**
     * Performs the blocking SolrJ calls inside a CompletableFuture so it can be consumed by Reactor.
     * For SolrJ 9.8, Http2SolrClient.request(UpdateRequest, collection) returns a NamedList which we
     * convert into UpdateResponse.
     */
    private CompletableFuture<UpdateResponse> indexDocumentAsync(PdfDocument pdfDocument) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id", pdfDocument.getId());
                doc.addField("filename", pdfDocument.getFilename());
                doc.addField("title", pdfDocument.getTitle());
                doc.addField("content", pdfDocument.getContent());
                doc.addField("created_date", pdfDocument.getCreatedDate());
                doc.addField("modified_date", pdfDocument.getModifiedDate());
                doc.addField("file_size", pdfDocument.getFileSize());
                doc.addField("page_count", pdfDocument.getPageCount());

                UpdateRequest req = new UpdateRequest();
                req.add(doc);

                // this is the correct way to get UpdateResponse
                return req.process(solrClient, collection);
            } catch (Exception e) {
                log.error("Error while indexing document {}: {}", pdfDocument.getFilename(), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * Delete all documents reactively (wrap blocking SolrJ calls in a CompletableFuture).
     */
    public Mono<Void> deleteAllDocuments() {
        return Mono.fromFuture(deleteAllAsync())
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("Deleted all documents in collection {}", collection))
                .doOnError(err -> log.error("Failed deleting documents in {}: {}", collection, err.getMessage()));
    }

    private CompletableFuture<UpdateResponse> deleteAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // deleteByQuery and commit are blocking SolrJ calls; call them here
                UpdateResponse deleteResp = solrClient.deleteByQuery(collection, "*:*");
                UpdateResponse commitResp = solrClient.commit(collection);
                // return commit response (or deleteResp if you prefer)
                return commitResp != null ? commitResp : deleteResp;
            } catch (Exception e) {
                log.error("Error while deleting all documents from collection {}: {}", collection, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
}
