package org.lucentrix.metaframe.reactive.crawler.event;

public class ProgressEvent {
    private String filename;
    private String status;
    // Getters and constructor
}

// Frontend (Vue.js) can connect to SSE endpoint:
// const eventSource = new EventSource('/api/push');
// eventSource.onmessage = (event) => { updateProgress(JSON.parse(event.data)); };
