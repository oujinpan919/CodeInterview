package com.codeinterview.ai.config;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 凭证加解密（D3：AES-256-GCM，08 §6.2）。
 *
 * <p>密文结构拆三列存库：{@code iv}(12B) / {@code tag}(16B) / {@code payload}(密文)。
 * GCM 同时提供机密性与完整性——被篡改的密文解密会抛异常，不会静默返回错误明文。
 *
 * <p>明文只在「写入凭证」与「发起上游请求」两个瞬间存在于内存，
 * 接口响应一律只回掩码（{@link #mask(String)}）。
 */
@Component
@RequiredArgsConstructor
public class CredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MIN_MASK_LEN = 6;

    private final MasterKeyProvider masterKeyProvider;
    private final SecureRandom random = new SecureRandom();

    /** 加密结果；三段都需要落库。 */
    public record Encrypted(byte[] iv, byte[] tag, byte[] payload) {
    }

    public Encrypted encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("凭证不能为空");
        }
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(masterKeyProvider.key(), ALGORITHM),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] combined = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            int payloadLength = combined.length - TAG_BITS / 8;
            byte[] payload = new byte[payloadLength];
            byte[] tag = new byte[TAG_BITS / 8];
            System.arraycopy(combined, 0, payload, 0, payloadLength);
            System.arraycopy(combined, payloadLength, tag, 0, tag.length);
            return new Encrypted(iv, tag, payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("凭证加密失败", ex);
        }
    }

    public String decrypt(byte[] iv, byte[] tag, byte[] payload) {
        if (iv == null || tag == null || payload == null) {
            throw new IllegalStateException("凭证密文不完整");
        }
        byte[] combined = new byte[payload.length + tag.length];
        System.arraycopy(payload, 0, combined, 0, payload.length);
        System.arraycopy(tag, 0, combined, payload.length, tag.length);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(masterKeyProvider.key(), ALGORITHM),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(combined), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            // 主密钥不匹配（换机/恢复备份）或密文被篡改都会走到这里
            throw new IllegalStateException("凭证解密失败：主密钥可能已变更，请重新填写该凭证", ex);
        }
    }

    /**
     * 生成掩码用于回显与日志：前 4 + **** + 后 4。
     * 长度不足 6 时整体打码，避免短 Key 被反推。
     */
    public String mask(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        String value = plaintext.trim();
        if (value.length() < MIN_MASK_LEN) {
            return "****";
        }
        int head = Math.min(4, value.length() / 3);
        int tail = Math.min(4, value.length() / 3);
        return value.substring(0, head) + "****" + value.substring(value.length() - tail);
    }

    /** 诊断用：确认主密钥可用而不泄露它。 */
    public boolean selfTest() {
        String probe = "codeinterview-probe";
        Encrypted encrypted = encrypt(probe);
        return probe.equals(decrypt(encrypted.iv(), encrypted.tag(), encrypted.payload()));
    }

    /** 从 Base64 代码值还原（测试/迁移用）。 */
    public static byte[] decodeBase64(String value) {
        return Base64.getDecoder().decode(value);
    }
}
