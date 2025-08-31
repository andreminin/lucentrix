package org.lucentrix.metaframe.encrypt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;


public class PasswordEncryptor {
    @JsonIgnore
    private final Cipher encryptor;
    @JsonIgnore
    private final Cipher decryptor;

    public PasswordEncryptor() {
        this(null);
    }

    public PasswordEncryptor(SecretSettings secretSettings) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            String secret;
            byte[] iv;
            byte[] salt;

            if (secretSettings == null) {
                salt = new byte[16];
                secureRandom.nextBytes(salt);

                iv = new byte[16];
                secureRandom.nextBytes(iv);

                secret = AESKeyGenerator.generateAESKeyEncoded(256);
            } else {
                if (StringUtils.isBlank(secretSettings.getSecret())) {
                    secret = AESKeyGenerator.generateAESKeyEncoded(256);
                } else {
                    secret = secretSettings.getSecret();
                }

                if (StringUtils.isBlank(secretSettings.getSalt())) {
                    salt = new byte[16];
                    secureRandom.nextBytes(salt);
                } else {
                    salt = Base64.getDecoder().decode(secretSettings.getSalt());
                }

                if (StringUtils.isBlank(secretSettings.getIv())) {
                    iv = new byte[16];
                    secureRandom.nextBytes(iv);
                } else {
                    iv = Base64.getDecoder().decode(secretSettings.getIv());
                }
            }


            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            SecretKey tmp = factory.generateSecret(new PBEKeySpec(secret.toCharArray(), salt, 65525, 256));
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            encryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
            encryptor.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            decryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decryptor.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        } catch (Exception ex) {
            throw new RuntimeException("Password encryptor initialization error", ex);
        }
    }

    @SneakyThrows
    public String encrypt(String plainPassword) {

        byte[] encrypted = encryptor.doFinal(plainPassword.getBytes());

        return "{"+Base64.getEncoder().encodeToString(encrypted)+"}";
    }

    @SneakyThrows
    public String decrypt(String encryptedText) {
        if(StringUtils.isBlank(encryptedText)) {
            return encryptedText;
        }

        if(!(encryptedText.startsWith("{") && encryptedText.endsWith("}"))) {
            return encryptedText;
        }

        byte[] encrypted;
        try {
            encrypted = Base64.getDecoder().decode(encryptedText.substring(1, encryptedText.length()-1));
        } catch (Exception ex) {
            throw new RuntimeException("Error decoding base64 text: "+encryptedText, ex);
        }

        try {
            return new String(decryptor.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("Error decrypting text: "+encryptedText, ex);
        }

    }
}
