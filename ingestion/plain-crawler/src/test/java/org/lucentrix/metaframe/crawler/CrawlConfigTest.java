package org.lucentrix.metaframe.crawler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CrawlConfigTest {


    @Test
    public void testConfig(@TempDir Path tempDir) throws IOException {
        InputStream is = getClass().getResourceAsStream("/config/dummy.json");
        assertNotNull(is);
        String dummyConfigText = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        CrawlConfig config = new CrawlConfig();

        assertEquals("crawler", config.getId());
        assertEquals(120, config.getStatisticsIntervalSec());
        assertEquals(30, config.getShutdownTimeoutSec());
        assertEquals(500, config.getSleepIntervalMsec());
        assertEquals(10000, config.getIdleSleepIntervalMsec());
        assertEquals("storage", config.getPersistencePath());
        assertEquals(1000, config.getSourcePageSize());

        Properties properties = new Properties();
        is = getClass().getResourceAsStream("/application.properties");
        assertNotNull(is);
        properties.load(is);

        config = new CrawlConfig(properties);
        assertEquals("crawler1", config.getId());
        assertEquals(121, config.getStatisticsIntervalSec());
        assertEquals(31, config.getShutdownTimeoutSec());
        assertEquals(501, config.getSleepIntervalMsec());
        assertEquals(10001, config.getIdleSleepIntervalMsec());
        assertEquals("persistence", config.getPersistencePath());
        assertEquals(1001, config.getSourcePageSize());

        assertEquals("QeLOFw7ETiBBcsQenVf5RMdTAqVvw9nXZdRw6wUvXs8=", config.getSecretSettings().getSecret());
        assertEquals("b+LEFfyuhkQEFgpWqlGm7Q==", config.getSecretSettings().getIv());
        assertEquals("3RXxBn1QGLIU841xdka0Ng==", config.getSecretSettings().getSalt());

        assertEquals("dummy-source-plugin", config.getSourcePluginId());
        assertEquals("solr-target-plugin", config.getTargetPluginId());

        assertEquals("classpath:/config/dummy.json", config.getPluginConfigPaths().get("dummy-source-plugin"));
        assertEquals("classpath:/config/solr.json", config.getPluginConfigPaths().get("solr-target-plugin"));

        Supplier<InputStream> supplier =  config.getPluginConfig("dummy-source-plugin");
        assertNotNull(supplier);
        assertNotNull(supplier.get());
        String text = new String(supplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertNotNull(text);

        assertEquals(dummyConfigText, text);

//        String path = getClass().getResource("/config/dummy.json").getFile();

        config.save(properties);

        File dummyConfigFile = new File(tempDir.toFile(), "dummy.json");
        try(FileOutputStream fos = new FileOutputStream(dummyConfigFile)) {
            fos.write(dummyConfigText.getBytes(StandardCharsets.UTF_8));
        }
        properties.setProperty("dummy-source-plugin.config", dummyConfigFile.getAbsolutePath());
        File solrConfigFile = new File(tempDir.toFile(), "solr.json");
        try(FileOutputStream fos = new FileOutputStream(solrConfigFile)) {
            fos.write(dummyConfigText.getBytes(StandardCharsets.UTF_8));
        }
        properties.setProperty("solr-target-plugin.config", solrConfigFile.getAbsolutePath());

        config = new CrawlConfig(properties);
        assertEquals(dummyConfigFile.getAbsolutePath(), config.getPluginConfigPaths().get("dummy-source-plugin"));
        supplier =  config.getPluginConfig("dummy-source-plugin");
        assertNotNull(supplier);
        assertNotNull(supplier.get());
        text = new String(supplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertNotNull(text);

        assertEquals(dummyConfigText, text);

        properties = new Properties();
        config.save(properties);

        File tempPropertiesFile = new File(tempDir.toFile(), "application.properties");
        properties.store(new FileOutputStream(tempPropertiesFile), "Sample comfig");

        properties = new Properties();
        properties.load(new FileInputStream(tempPropertiesFile));
        config = new CrawlConfig(properties);
        assertEquals("crawler1", config.getId());
        assertEquals(121, config.getStatisticsIntervalSec());
        assertEquals(31, config.getShutdownTimeoutSec());
        assertEquals(501, config.getSleepIntervalMsec());
        assertEquals(10001, config.getIdleSleepIntervalMsec());
        assertEquals("persistence", config.getPersistencePath());
        assertEquals(1001, config.getSourcePageSize());

        assertEquals("QeLOFw7ETiBBcsQenVf5RMdTAqVvw9nXZdRw6wUvXs8=", config.getSecretSettings().getSecret());
        assertEquals("b+LEFfyuhkQEFgpWqlGm7Q==", config.getSecretSettings().getIv());
        assertEquals("3RXxBn1QGLIU841xdka0Ng==", config.getSecretSettings().getSalt());

        assertEquals("dummy-source-plugin", config.getSourcePluginId());
        assertEquals("solr-target-plugin", config.getTargetPluginId());

        assertEquals(dummyConfigFile.getAbsolutePath(), config.getPluginConfigPaths().get("dummy-source-plugin"));
        assertEquals(solrConfigFile.getAbsolutePath(), config.getPluginConfigPaths().get("solr-target-plugin"));

    }
}
