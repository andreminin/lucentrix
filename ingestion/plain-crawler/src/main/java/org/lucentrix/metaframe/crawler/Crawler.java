package org.lucentrix.metaframe.crawler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Crawler {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream("application.properties")) {
            props.load(is);
        } catch (Exception ex) {
            throw new RuntimeException("Error loading application.properties", ex);
        }

        CrawlConfig config = new CrawlConfig(props);

        CrawlContext context = new CrawlContext(config);

        try {
            CrawlService crawlService = new CrawlService(context);

            crawlService.run();
        } catch (Exception ex) {
            logger.error("Critical error, system exit", ex);
            System.exit(1);
        }
    }
}
