package org.lucentrix.metaframe.plugin.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lucentrix.metaframe.DocumentPage;
import org.lucentrix.metaframe.LxAction;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.metadata.Cursor;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.lucentrix.metaframe.runtime.DocumentRetriever;
import org.lucentrix.metaframe.runtime.plugin.AbstractPlugin;
import org.lucentrix.metaframe.runtime.plugin.RetrieverPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@ToString
@EqualsAndHashCode(callSuper = true)
public class WebSourcePlugin extends AbstractPlugin<WebSourceConfig, RetrieverPluginContext> implements DocumentRetriever {
    private final Logger logger = LoggerFactory.getLogger(WebSourcePlugin.class);

    public static final Field<String> rawHtmlField = Field.of("$raw_html", FieldType.STRING);
    public static final Field<String> urlField = Field.of("$url", FieldType.STRING);
    public static final Field<List<String>> visitedUrlsField = Field.of("$visited_urls", FieldType.STRING_LIST);
    public static final Field<List<String>> crawlPointUrlsField = Field.of("$crawl_point_urls", FieldType.STRING_LIST);

    private Cursor cursor;

    private final Queue<UrlDepth> crawlPointUrls = new LinkedList<>();
    private final Set<String> visited = new HashSet<>();

    private final LoadingCache<String, Field<String>> nameToFieldCache = CacheBuilder.newBuilder()
            .maximumSize(10000).initialCapacity(100)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Field<String> load(String name) {
                            return Field.of(name, TypeId.STRING);
                        }
                    }
            );

    public WebSourcePlugin(WebSourceConfig config, RetrieverPluginContext context) {
        super(config, context);

        if (getPersistence() == null) {
            throw new IllegalArgumentException("Cursor persistence is null!");
        }
    }

    @Override
    public void start() {
        super.start();

        String name = config.getName();

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("DummySourceConfig name field is blank or null!");
        }

        this.cursor = loadCursor(name);

        List<String> visitedUrls = this.cursor.getFields().get(visitedUrlsField, new ArrayList<>());
        List<String> crawlPoints = this.cursor.getFields().get(crawlPointUrlsField, new ArrayList<>());
        if (crawlPoints.isEmpty()) {
            crawlPoints.add(config.getSettings().getUrl());
        }
        crawlPointUrls.clear();
        crawlPointUrls.addAll(crawlPoints.stream().map(UrlDepth::new).toList());

        visited.addAll(visitedUrls);

        logger.info("Dummy document source started: {}", this);
    }

    private Cursor loadCursor(String name) {
        Cursor.SuffixCursor suffixCursor = getPersistence().load(name, Cursor.SuffixCursor.class);
        if (suffixCursor != null) {
            return Cursor.SuffixCursor.toCursor(suffixCursor);
        }

        Cursor cursor = new Cursor(name);
        suffixCursor = Cursor.SuffixCursor.toSuffixCursor(cursor);
        getPersistence().save(suffixCursor);

        return cursor;
    }

    @Override
    public boolean hasNext() {
        return !this.crawlPointUrls.isEmpty();
    }

    @Override
    public DocumentPage next() {
        if (!hasNext()) {
            return DocumentPage.builder().cursor(cursor).items(Collections.emptyList()).build();
        }

        List<LxEvent> events = new ArrayList<>();

        while (!crawlPointUrls.isEmpty() && events.size() < context.getPageSize()) {
            UrlDepth urlDepth = crawlPointUrls.poll();
            assert urlDepth != null;
            int depth = urlDepth.getDepth();
            String url = urlDepth.getUrl();

            if (visited.contains(url)) continue;

            try {
                LxEvent event = crawlPage(url);
                events.add(event);
                visited.add(url);

                if (depth < config.getSettings().getMaxDepth()) {
                    List<String> discovered = extractLinks(url, event.getDocument().get(rawHtmlField));
                    for (String link : discovered) {
                        if (!visited.contains(link)) {
                            crawlPointUrls.offer(new UrlDepth(link, depth + 1));
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to crawl url: " + url, e);
            }
        }

        if (!events.isEmpty()) {
            List<String> urlAndDepths = crawlPointUrls.stream().map(urlAndDepth -> urlAndDepth.getUrl() + "|" + urlAndDepth.getDepth()).toList();
            this.cursor = this.cursor.toBuilder()
                    .fields(this.cursor.getFields().toBuilder()
                            .field(visitedUrlsField, new ArrayList<>(visited))
                            .field(crawlPointUrlsField, urlAndDepths)
                            .build())
                    .build();
        }

        return DocumentPage.builder().items(events).cursor(cursor).hasNext(hasNext()).build();
    }

    @Override
    public void save() {
        Cursor.SuffixCursor suffixCursor = Cursor.SuffixCursor.toSuffixCursor(cursor);
        getPersistence().save(suffixCursor);
    }

    private LxEvent crawlPage(String url) throws IOException {
        LxDocument.LxDocumentBuilder<?, ?> builder = LxDocument.builder();

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; JsoupCrawler/1.0)")
                .timeout(10_000)
                .get();

        builder.id(generateUUIDFromURL(url).toString());
        builder.field(urlField, url);
        builder.title(doc.title());

        // Meta tags
        Elements metaTags = doc.select("meta[name], meta[property]");
        for (Element meta : metaTags) {
            String name = meta.hasAttr("name") ? meta.attr("name") : meta.attr("property");
            String content = meta.attr("content");
            if (!name.isEmpty() && !content.isEmpty()) {
                builder.field(nameToFieldCache.getUnchecked(name.toLowerCase()), content);
            }
        }

        builder.content(doc.body().text());
        builder.field(rawHtmlField, doc.html()); // for internal use

        return LxEvent.builder().action(LxAction.REPLACE).document(builder.build()).build();
    }

    public static UUID generateUUIDFromURL(String url) {
        try {
            // UUIDv5 namespace for URLs: 6ba7b811-9dad-11d1-80b4-00c04fd430c8
            UUID namespace = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");

            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(toBytes(namespace));
            sha1.update(url.getBytes(StandardCharsets.UTF_8));
            byte[] hash = sha1.digest();

            hash[6] &= 0x0f;  // clear version
            hash[6] |= 0x50;  // set to version 5
            hash[8] &= 0x3f;  // clear variant
            hash[8] |= 0x80;  // set to IETF variant

            long msb = 0, lsb = 0;
            for (int i = 0; i < 8; i++) msb = (msb << 8) | (hash[i] & 0xff);
            for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (hash[i] & 0xff);

            return new UUID(msb, lsb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate UUID from URL", e);
        }
    }

    private static byte[] toBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer.wrap(bytes)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return bytes;
    }

    private static List<String> extractLinks(String baseUrl, String html) throws IOException {
        List<String> links = new ArrayList<>();
        Document doc = Jsoup.parse(html, baseUrl);

        for (Element link : doc.select("a[href]")) {
            String absHref = link.attr("abs:href");
            if (absHref.startsWith("http")) {
                links.add(absHref.split("#")[0]); // drop anchors
            }
        }

        return links;
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class UrlDepth {
        public final String url;
        public final int depth;

        public UrlDepth(String urlAndDepth) {
            int pos = urlAndDepth.lastIndexOf("|");
            if (pos > 0) {
                this.url = urlAndDepth.substring(0, pos);
                String depthTxt = urlAndDepth.substring(pos + 1);
                this.depth = StringUtils.isBlank(depthTxt) ? 0 : Integer.parseInt(depthTxt);
            } else {
                this.url = urlAndDepth;
                this.depth = 0;
            }
        }
    }
}
