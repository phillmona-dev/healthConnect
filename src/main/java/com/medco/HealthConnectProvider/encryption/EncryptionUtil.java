package com.medco.HealthConnectProvider.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private final String secretKey;
    private final String algorithm;

    public EncryptionUtil(
            @Value("${encryption.aes.secret-key}") String secretKey,
            @Value("${encryption.aes.algorithm}") String algorithm) {
        this.secretKey = secretKey;
        this.algorithm = algorithm;

        if (secretKey.length() != 16 && secretKey.length() != 24 && secretKey.length() != 32) {
            throw new IllegalArgumentException(
                    "AES key must be 16, 24, or 32 bytes long. Current length: " + secretKey.length());
        }
    }

    public String encrypt(String value) {
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
}
