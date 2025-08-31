package org.lucentrix.metaframe.encrypt;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AESKeyGenerator {

    public static void main(String[] args) {
        try {
            SecretKey secretKey = generateAESKey(256);
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            System.out.println("Generated AES key (Base64 encoded): " + encodedKey);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error generating key: " + e.getMessage());
        }
    }

    public static SecretKey generateAESKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        return keyGen.generateKey();
    }

    public static String generateAESKeyEncoded(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        return Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
    }
}
