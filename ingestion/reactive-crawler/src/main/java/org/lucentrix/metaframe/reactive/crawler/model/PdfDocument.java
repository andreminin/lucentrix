package org.lucentrix.metaframe.reactive.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfDocument {
    private String id;
    private String filename;
    private String title;
    private String content;
    private Instant createdDate;
    private Instant modifiedDate;
    private long fileSize;
    private int pageCount;
    private ProcessingStatus status;
    private String errorMessage;

    public enum ProcessingStatus {
        PENDING, PROCESSING, INDEXED, FAILED
    }
}