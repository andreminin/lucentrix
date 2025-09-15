package org.lucentrix.metaframe.reactive.crawler.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.lucentrix.metaframe.reactive.crawler.model.PdfDocument;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class PdfProcessingService {

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