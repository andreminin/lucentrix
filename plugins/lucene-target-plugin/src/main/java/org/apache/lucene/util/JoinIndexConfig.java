package org.apache.lucene.util;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Builder
@Getter
public class JoinIndexConfig {
    private final JoinIndexType type;
    private final String indexName;
    private final Path diskPath;
    private final boolean autoBuild;


}
