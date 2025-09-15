package org.lucentrix.metaframe.reactive.crawler.config;

import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class SolrConfig {

    @Value("${solr.url:http://localhost:8983/solr}")
    private String solrUrl;

    @Value("${solr.collection:pdf_documents}")
    private String solrCollection;

    @Bean
    public Http2SolrClient solrClient() {
        return new Http2SolrClient.Builder(solrUrl)
                .withConnectionTimeout(10000, TimeUnit.MILLISECONDS)
                .withIdleTimeout(60000, TimeUnit.MILLISECONDS)
                .build();
    }

    public String getSolrCollection() {
        return solrCollection;
    }
}

