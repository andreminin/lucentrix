package org.lucentrix.metaframe.plugin.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.plugin.web.WebSourceConfig;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebSourceConfigTest {


    @Test
    public void testConfigReadWrite(@TempDir Path tempDir) {

        WebSourceConfig.Settings settings = WebSourceConfig.Settings.builder()
                .name("WebTestSource")
                .url("https://python.langchain.com/docs/tutorials/rag/")
                .maxDepth(5)
                .build();

        ConfigEnv configEnv = ConfigEnv.builder()
                .encryptor(new PasswordEncryptor())
                .build();

        WebSourceConfig configFromCode = new WebSourceConfig(settings, configEnv);

        File configFile = new File(tempDir.toFile(), "web.json");

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            configFromCode.save(fos);
        } catch (Exception ex) {
            throw new RuntimeException("Error saving config", ex);
        }

        WebSourceConfig configFromFile;
        try(FileInputStream fis = new FileInputStream(configFile)) {
            configFromFile = new WebSourceConfig(fis, configEnv);
        }catch (Exception ex) {
            throw new RuntimeException("Error reading config", ex);
        }

        assertEquals(configFromCode, configFromFile);
    }
}
