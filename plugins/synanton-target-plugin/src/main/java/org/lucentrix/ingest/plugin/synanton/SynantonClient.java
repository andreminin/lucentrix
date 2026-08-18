package org.lucentrix.ingest.plugin.synanton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class SynantonClient {

    private static final Logger log = LoggerFactory.getLogger(SynantonClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String vaultBase;
    private final String synfluxUrl;
    private final String tenant;
    private final String apiKey;

    SynantonClient(String vaultBase, String synfluxUrl, String tenant, String apiKey) {
        this.vaultBase = trimSlash(vaultBase);
        this.synfluxUrl = trimSlash(synfluxUrl);
        this.tenant = tenant;
        this.apiKey = apiKey;
    }

    UUID push(byte[] bytes, String sourceUri, String mimeType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(vaultBase + "/content/" + tenant))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", mimeType)
                    .header("X-Tenant", tenant)
                    .header("X-Source-Uri", sourceUri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes));
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("synvault push failed: " + response.statusCode() + " " + response.body());
            }
            JsonNode node = MAPPER.readTree(response.body());
            return UUID.fromString(node.get("content_ref_id").asText());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted pushing to synvault", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed pushing content to synvault", ex);
        }
    }

    void enqueue(List<UUID> refs) {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        String path = refs.stream().map(UUID::toString).collect(Collectors.joining(","));
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                    "tenant", tenant,
                    "source", "synvault",
                    "path", path
            ));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(synfluxUrl + "/ingest"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-Tenant", tenant)
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("synflux enqueue failed: " + response.statusCode() + " " + response.body());
            }
            log.info("Enqueued {} content refs with synflux", refs.size());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted enqueueing synflux job", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed enqueueing synflux job", ex);
        }
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
