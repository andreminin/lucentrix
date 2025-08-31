package org.lucentrix.metaframe.runtime.plugin;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.lucentrix.metaframe.encrypt.PasswordEncryptor;

@Getter
@EqualsAndHashCode
@Builder
public class ConfigEnv {
    PasswordEncryptor encryptor;
}
