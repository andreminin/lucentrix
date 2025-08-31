package org.lucentrix.metaframe.encrypt;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecretSettings {
    private String secret;
    private String salt;
    private String iv;
}
