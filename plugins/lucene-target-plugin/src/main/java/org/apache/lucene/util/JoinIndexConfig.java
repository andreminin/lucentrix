// JoinIndexConfig.java - добавляем version tracking
package org.apache.lucene.util;

import java.nio.file.Path;

public class JoinIndexConfig {
    private final JoinIndexType type;
    private final String indexName;
    private final Path diskPath;
    private final boolean autoBuild;
    private final long maxMemoryBytes;
    private final long timeToLiveMs;

    public JoinIndexConfig(JoinIndexType type, String indexName, Path diskPath,
                           boolean autoBuild, long maxMemoryBytes, long timeToLiveMs) {
        this.type = type;
        this.indexName = indexName;
        this.diskPath = diskPath;
        this.autoBuild = autoBuild;
        this.maxMemoryBytes = maxMemoryBytes;
        this.timeToLiveMs = timeToLiveMs;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JoinIndexType type;
        private String indexName;
        private Path diskPath;
        private boolean autoBuild = true;
        private long maxMemoryBytes = 100 * 1024 * 1024; // 100MB default
        private long timeToLiveMs = 300_000; // 5 minutes default

        public Builder type(JoinIndexType type) { this.type = type; return this; }
        public Builder indexName(String indexName) { this.indexName = indexName; return this; }
        public Builder diskPath(Path diskPath) { this.diskPath = diskPath; return this; }
        public Builder autoBuild(boolean autoBuild) { this.autoBuild = autoBuild; return this; }
        public Builder maxMemoryBytes(long maxMemoryBytes) { this.maxMemoryBytes = maxMemoryBytes; return this; }
        public Builder timeToLiveMs(long timeToLiveMs) { this.timeToLiveMs = timeToLiveMs; return this; }

        public JoinIndexConfig build() {
            return new JoinIndexConfig(type, indexName, diskPath, autoBuild, maxMemoryBytes, timeToLiveMs);
        }
    }

    // Getters
    public JoinIndexType getType() { return type; }
    public String getIndexName() { return indexName; }
    public Path getDiskPath() { return diskPath; }
    public boolean isAutoBuild() { return autoBuild; }
    public long getMaxMemoryBytes() { return maxMemoryBytes; }
    public long getTimeToLiveMs() { return timeToLiveMs; }
}