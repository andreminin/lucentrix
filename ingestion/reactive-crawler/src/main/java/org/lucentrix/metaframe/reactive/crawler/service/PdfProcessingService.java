package org.lucentrix.metaframe.reactive.crawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.lucentrix.metaframe.reactive.crawler.config.CrawlerProperties;
import org.lucentrix.metaframe.reactive.crawler.model.PdfDocument;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
@Slf4j
@Service
public class PdfProcessingService {

    private final CrawlerProperties properties;

    /**
     * Download PDFs from configured seed URLs reactively.
     */
    public Flux<byte[]> downloadSeedPdfs() {
        return Flux.fromIterable(properties.getSeedUrls())
                .flatMap(this::fetchPdf)
                .doOnNext(content -> log.info("Downloaded PDF with size: {}", content.length))
                .doOnError(error -> log.error("Error fetching PDF", error));
    }

    private Mono<byte[]> fetchPdf(String url) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(properties.getRequestTimeout())
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(properties.getRequestTimeout())
                .build();

        return Mono.fromCompletionStage(client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()))
                .map(HttpResponse::body)
                .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(2)))
                .doOnError(e -> log.warn("Failed to download {}: {}", url, e.getMessage()));
    }

    public Mono<PdfDocument> processPdfFile(Path filePath) {
        return Mono.fromFuture(processPdfAsync(filePath))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(subscription -> log.info("Processing PDF: {}", filePath.getFileName()))
                .doOnSuccess(doc -> log.info("PDF processed successfully: {}", filePath.getFileName()))
                .doOnError(error -> log.error("PDF processing failed for {}: {}",
                        filePath.getFileName(), error.getMessage()));
    }

    private CompletableFuture<PdfDocument> processPdfAsync(Path filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
                PDDocumentInformation info = document.getDocumentInformation();
                PDFTextStripper stripper = new PDFTextStripper();

                PdfDocument pdfDocument = new PdfDocument();
                pdfDocument.setId(java.util.UUID.randomUUID().toString());
                pdfDocument.setFilename(filePath.getFileName().toString());
                pdfDocument.setTitle(info.getTitle() != null ? info.getTitle() : filePath.getFileName().toString());
                pdfDocument.setContent(stripper.getText(document));
                pdfDocument.setCreatedDate(info.getCreationDate() != null ?
                        info.getCreationDate().toInstant() : Instant.now());
                pdfDocument.setModifiedDate(info.getModificationDate() != null ?
                        info.getModificationDate().toInstant() : Instant.now());
                pdfDocument.setFileSize(Files.size(filePath));
                pdfDocument.setPageCount(document.getNumberOfPages());
                pdfDocument.setStatus(PdfDocument.ProcessingStatus.PROCESSING);

                return pdfDocument;
            } catch (IOException e) {
                PdfDocument failedDoc = new PdfDocument();
                failedDoc.setFilename(filePath.getFileName().toString());
                failedDoc.setStatus(PdfDocument.ProcessingStatus.FAILED);
                failedDoc.setErrorMessage(e.getMessage());
                throw new RuntimeException("Failed to process PDF: " + filePath.getFileName(), e);
            }
        });
    }
}