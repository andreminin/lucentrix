package org.lucentrix.metaframe.plugin.solr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SolrTargetConfigTest {
    PasswordEncryptor encryptor = new PasswordEncryptor();

    @Test
    public void testSolrConfig() {
        String password = "Secret password";
        String encryptedPassword = encryptor.encrypt(password);

        assertNotEquals(password, encryptedPassword);

        String decrypted = encryptor.decrypt(encryptedPassword);

        assertEquals(password, decrypted);
    }

    @Test
    public void testConfigReadWrite(@TempDir Path tempDir) {
        String password = "Secret password";
        String encryptedPassword = encryptor.encrypt(password);

        ConfigEnv configEnv = ConfigEnv.builder()
                .encryptor(encryptor)
                .build();

        SolrTargetConfig configFromCode = new SolrTargetConfig(SolrTargetConfig.Settings.builder()
                .solrCommitOptions(SolrCommitOptions.builder()
                        .commitWithin(-1)
                        .waitForFlush(false)
                        .softCommit(false)
                        .build())
                .name("SolrTestTarget")
                .solrUrls(List.of("http://solr1:8983/solr", "http://solr2:8983/solr", "http://solr3:8983/solr"))
                .collection("samples")
                .autoCommitPolicy(AutoCommitPolicy.builder()
                        .commitDocCount(10000)
                        .enabled(true)
                        .commitIntervalMs(20000L)
                        .build())
                .user("user")
                .password(encryptedPassword)
                .zkHosts(List.of("zookeeper1:2181","zookeeper2:2181","zookeeper3:2181"))
                .zkChroot("root")
                .fieldMappings(Set.of(
                        SolrFieldMapping.builder().docField(Field.ID).solrField("id").build(),
                        SolrFieldMapping.builder().docField(Field.CONTENT).solrField("content_tt").build()
                ))
                .build(), configEnv);

        File configFile = new File(tempDir.toFile(), "config.json");

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            configFromCode.save(fos);
        } catch (Exception ex) {
            throw new RuntimeException("Error saving config", ex);
        }

        SolrTargetConfig configFromFile;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            configFromFile = new SolrTargetConfig(fis, configEnv);
        } catch (Exception ex) {
            throw new RuntimeException("Error reading config", ex);
        }

        assertEquals(configFromCode, configFromFile);
    }
}
