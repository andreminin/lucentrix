package org.lucentrix.ingest.runtime.plugin;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.lucentrix.ingest.encrypt.PasswordEncryptor;

@Getter
@EqualsAndHashCode
@Builder
public class ConfigEnv {
    PasswordEncryptor encryptor;
}
