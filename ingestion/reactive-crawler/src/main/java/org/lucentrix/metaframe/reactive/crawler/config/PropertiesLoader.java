package org.lucentrix.metaframe.reactive.crawler.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Minimal properties' loader to support projects that do not use Spring Boot.
 * Loads ingestion/reactive-crawler/src/main/resources/crawler.properties if present,
 * and falls back to default.
 */
public final class PropertiesLoader {
    private static final String PROPS = "/crawler.properties";
    private final Properties p = new Properties();

    public PropertiesLoader() {
        try (InputStream is = getClass().getResourceAsStream(PROPS)) {
            if (is != null) {
                p.load(is);
            }
        } catch (IOException e) {
            // ignore and use defaults
        }
    }

    public List<String> getSeeds() {
        String s = p.getProperty("crawler.seeds");
        if (s == null || s.isBlank()) {
            return Collections.singletonList("https://example.org/");
        }
        return Arrays.asList(s.split(","));
    }

    public int getConcurrency() {
        return Integer.parseInt(p.getProperty("crawler.concurrency", "8"));
    }

    public int getConnectTimeoutMs() {
        return Integer.parseInt(p.getProperty("crawler.connectTimeoutMs", "5000"));
    }


}
