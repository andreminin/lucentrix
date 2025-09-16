package org.lucentrix.metaframe.reactive.crawler.controller;


import lombok.extern.slf4j.Slf4j;
import org.lucentrix.metaframe.reactive.crawler.model.PdfDocument;
import org.lucentrix.metaframe.reactive.crawler.service.PdfProcessingService;
import org.lucentrix.metaframe.reactive.crawler.service.ReactiveSolrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final PdfProcessingService pdfProcessingService;
    private final ReactiveSolrService reactiveSolrService;

    private final Map<String, PdfDocument.ProcessingStatus> processingStatus = new ConcurrentHashMap<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);

    @Autowired
    public CrawlerController(PdfProcessingService pdfProcessingService,
                             ReactiveSolrService reactiveSolrService) {
        this.pdfProcessingService = pdfProcessingService;
        this.reactiveSolrService = reactiveSolrService;
    }

    @GetMapping("/seed")
    public Flux<String> crawl() {
        return pdfProcessingService.downloadSeedPdfs()
                .map(bytes -> "Downloaded PDF size: " + bytes.length);
    }

    // Update the push endpoint
    @PutMapping(value = "/push", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<ServerSentEvent<String>> pushPdfFiles(@RequestPart("files") Flux<FilePart> files) {
        resetCounters();

        return files.flatMap(file -> {
            String filename = file.filename();
            Path tempFile = Paths.get("/tmp/" + filename);

            return file.transferTo(tempFile)
                    .then(Mono.defer(() -> {
                        processingStatus.put(filename, PdfDocument.ProcessingStatus.PROCESSING);
                        totalCount.incrementAndGet();

                        return pdfProcessingService.processPdfFile(tempFile)
                                .flatMap(pdfDocument -> {
                                    processingStatus.put(filename, PdfDocument.ProcessingStatus.INDEXED);
                                    return reactiveSolrService.indexDocument(pdfDocument);
                                })
                                .doOnSuccess(result -> {
                                    processedCount.incrementAndGet();
                                    log.info("Successfully processed: {}", filename);
                                })
                                .doOnError(error -> {
                                    processingStatus.put(filename, PdfDocument.ProcessingStatus.FAILED);
                                    processedCount.incrementAndGet();
                                    log.error("Failed to process {}: {}", filename, error.getMessage());
                                })
                                .onErrorResume(e -> Mono.just("Error processing " + filename + ": " + e.getMessage()))
                                .doFinally(signal -> {
                                    try {
                                        Files.deleteIfExists(tempFile);
                                    } catch (IOException e) {
                                        log.warn("Could not delete temporary file: {}", tempFile, e);
                                    }
                                });
                    }));
        }).flatMap(message ->
                Flux.interval(Duration.ofMillis(100))
                        .map(seq -> ServerSentEvent.<String>builder()
                                .id(String.valueOf(seq))
                                .event("progress")
                                .data("Processed " + processedCount.get() + " of " + totalCount.get())
                                .build())
        );
    }

    @PostMapping("/scan")
    public Flux<PdfDocument> scanDirectory(@RequestParam(value = "url", defaultValue = "./documents") String directoryPath) {
        resetCounters();

        try {
            return Flux.fromStream(Files.list(Paths.get(directoryPath)))
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .flatMap(filePath -> {
                        String filename = filePath.getFileName().toString();
                        processingStatus.put(filename, PdfDocument.ProcessingStatus.PROCESSING);
                        totalCount.incrementAndGet();

                        return pdfProcessingService.processPdfFile(filePath)
                                .flatMap(pdfDocument -> {
                                    processingStatus.put(filename, PdfDocument.ProcessingStatus.INDEXED);
                                    return reactiveSolrService.indexDocument(pdfDocument);
                                })
                                .then(Mono.defer(() -> {
                                    processedCount.incrementAndGet();
                                    return pdfProcessingService.processPdfFile(filePath);
                                }))
                                .doOnError(error -> {
                                    processingStatus.put(filename, PdfDocument.ProcessingStatus.FAILED);
                                    processedCount.incrementAndGet();
                                    log.error("Failed to process {}: {}", filename, error.getMessage());
                                })
                                .onErrorResume(e -> Mono.empty());
                    });
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to scan directory: " + directoryPath, e));
        }
    }

    @GetMapping(value = "/pull", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> pullPdfMetadata(@RequestParam(value = "url", defaultValue = "./documents") String directoryPath) {
        resetCounters();

        try {
            return Flux.fromStream(Files.list(Paths.get(directoryPath)))
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .delayElements(Duration.ofMillis(500)) // Simulate processing time
                    .flatMap(filePath -> pdfProcessingService.processPdfFile(filePath))
                    .map(pdfDocument -> {
                        processedCount.incrementAndGet();
                        return ServerSentEvent.<Map<String, Object>>builder()
                                .id(pdfDocument.getId())
                                .event("document")
                                .data(Map.of(
                                        "filename", pdfDocument.getFilename(),
                                        "title", pdfDocument.getTitle(),
                                        "pageCount", pdfDocument.getPageCount(),
                                        "fileSize", pdfDocument.getFileSize(),
                                        "processed", processedCount.get()
                                ))
                                .build();
                    });
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to read directory: " + directoryPath, e));
        }
    }

    @GetMapping("/progress")
    public Flux<ServerSentEvent<Map<String, Object>>> getProgress() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq -> ServerSentEvent.<Map<String, Object>>builder()
                        .id(String.valueOf(seq))
                        .event("progress")
                        .data(Map.of(
                                "processed", processedCount.get(),
                                "total", totalCount.get(),
                                "timestamp", LocalTime.now().toString()
                        ))
                        .build());
    }

    @DeleteMapping("/clear")
    public Mono<ResponseEntity<String>> clearIndex() {
        return reactiveSolrService.deleteAllDocuments()
                .then(Mono.just(ResponseEntity.ok("All documents cleared from index")))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to clear index: " + e.getMessage())));
    }

    private void resetCounters() {
        processedCount.set(0);
        totalCount.set(0);
        processingStatus.clear();
    }
}