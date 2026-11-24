package com.codeinterview.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.codeinterview.ai.config.CredentialCipher;
import com.codeinterview.ai.config.MasterKeyProvider;

/**
 * T21 / 08 §6.2 / §7 S2/S4：AES-GCM 凭证加解密与主密钥管理。
 */
class CredentialCipherTest {

    private MasterKeyProvider providerWithKey(String keyBase64) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(MasterKeyProvider.ENV_KEY, keyBase64);
        return new MasterKeyProvider(environment);
    }

    private String randomKey() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    @DisplayName("加密后可解密，且密文不含明文")
    void encryptThenDecryptRoundTrips() {
        CredentialCipher cipher = new CredentialCipher(providerWithKey(randomKey()));
        String plaintext = "ak-1234567890abcdef-secret";

        CredentialCipher.Encrypted encrypted = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(encrypted.iv(), encrypted.tag(), encrypted.payload());

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(new String(encrypted.payload(), java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain(plaintext);
    }

    @Test
    @DisplayName("相同明文两次加密产生不同密文（随机 IV）")
    void encryptionIsRandomized() {
        CredentialCipher cipher = new CredentialCipher(providerWithKey(randomKey()));

        CredentialCipher.Encrypted first = cipher.encrypt("same-secret");
        CredentialCipher.Encrypted second = cipher.encrypt("same-secret");

        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(first.payload()).isNotEqualTo(second.payload());
    }

    @Test
    @DisplayName("换主密钥后解密失败（GCM 完整性校验，不会返回错误明文）")
    void wrongMasterKeyFailsLoudly() {
        CredentialCipher encoder = new CredentialCipher(providerWithKey(randomKey()));
        CredentialCipher.Encrypted encrypted = encoder.encrypt("secret");

        CredentialCipher decoder = new CredentialCipher(providerWithKey(randomKey()));

        assertThatThrownBy(() -> decoder.decrypt(encrypted.iv(), encrypted.tag(), encrypted.payload()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("主密钥");
    }

    @Test
    @DisplayName("密文被篡改时解密失败")
    void tamperedCiphertextFails() {
        CredentialCipher cipher = new CredentialCipher(providerWithKey(randomKey()));
        CredentialCipher.Encrypted encrypted = cipher.encrypt("secret");
        byte[] tampered = encrypted.payload().clone();
        tampered[0] = (byte) (tampered[0] ^ 0xFF);

        assertThatThrownBy(() -> cipher.decrypt(encrypted.iv(), encrypted.tag(), tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("掩码只露首尾，短 Key 整体打码")
    void maskHidesMiddle() {
        CredentialCipher cipher = new CredentialCipher(providerWithKey(randomKey()));

        assertThat(cipher.mask("ak-1234567890abcdef")).isEqualTo("ak-1****cdef");
        assertThat(cipher.mask("abc")).isEqualTo("****");
        assertThat(cipher.mask("")).isEmpty();
    }

    @Test
    @DisplayName("主密钥支持 64 位十六进制写法")
    void acceptsHexKey() {
        String hex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        CredentialCipher cipher = new CredentialCipher(providerWithKey(hex));

        CredentialCipher.Encrypted encrypted = cipher.encrypt("secret");
        assertThat(cipher.decrypt(encrypted.iv(), encrypted.tag(), encrypted.payload())).isEqualTo("secret");
    }

    @Test
    @DisplayName("主密钥长度不对时启动即失败（不静默降级）")
    void rejectsWrongKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> providerWithKey(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("selfTest 能验证主密钥可用")
    void selfTestPasses() {
        CredentialCipher cipher = new CredentialCipher(providerWithKey(randomKey()));
        assertThat(cipher.selfTest()).isTrue();
    }
}
