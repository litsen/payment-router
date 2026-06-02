package com.company.payrouter.infrastructure.crypto;

import com.company.payrouter.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesCryptoService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final CryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesCryptoService(CryptoProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new BizException("Sensitive parameter encryption failed");
        }
    }

    public String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BizException("Sensitive parameter decryption failed");
        }
    }

    public String maskEncrypted(String encryptedText) {
        return mask(decrypt(encryptedText));
    }

    public String mask(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        int length = plainText.length();
        if (length <= 4) {
            return "****";
        }
        if (length <= 8) {
            return plainText.substring(0, 2) + "****" + plainText.substring(length - 2);
        }
        return plainText.substring(0, 4) + "****" + plainText.substring(length - 4);
    }

    private SecretKeySpec keySpec() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(properties.getAesKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
