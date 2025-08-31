package org.lucentrix.metaframe.plugin.dummy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;
import org.lucentrix.metaframe.runtime.plugin.ConfigEnv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DummySourceConfigTest {


    @Test
    public void testConfigReadWrite(@TempDir Path tempDir) {

        DummySourceConfig.Settings settings = DummySourceConfig.Settings.builder()
                .name("DummyTestSource")
                .claimMaxCount(20 * 1000 * 1000 - 4)
                .clientMaxCount(5 * 1000 * 1000 + 3)
                .policyMaxCount(100 * 1000 + 2)
                .securityMaxCount(501)
                .userCount(1001)
                .groupCount(51)
                .build();

        ConfigEnv configEnv = ConfigEnv.builder()
                .encryptor(new PasswordEncryptor())
                .build();

        DummySourceConfig configFromCode = new DummySourceConfig(settings, configEnv);

        File configFile = new File(tempDir.toFile(), "config.json");

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            configFromCode.save(fos);
        } catch (Exception ex) {
            throw new RuntimeException("Error saving config", ex);
        }

        DummySourceConfig configFromFile;
        try(FileInputStream fis = new FileInputStream(configFile)) {
            configFromFile = new DummySourceConfig(fis, configEnv);
        }catch (Exception ex) {
            throw new RuntimeException("Error reading config", ex);
        }

        assertEquals(configFromCode, configFromFile);
    }
}
