package com.codeinterview.ai.service;

import org.springframework.stereotype.Service;

import com.codeinterview.ai.config.CredentialCipher;
import com.codeinterview.ai.config.MasterKeyProvider;
import com.codeinterview.ai.entity.AiCredential;
import com.codeinterview.ai.mapper.AiCredentialMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 凭证读写（D3）。明文只在两个瞬间存在：写入时、发起上游请求时。
 * 对外只暴露掩码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final AiCredentialMapper credentialMapper;
    private final CredentialCipher cipher;
    private final MasterKeyProvider masterKeyProvider;

    /**
     * 新增或更新凭证。{@code credentialId} 为空表示新建。
     *
     * @return 凭证 id
     */
    public Long save(Long credentialId, int ownerType, long ownerId, String plaintext) {
        CredentialCipher.Encrypted encrypted = cipher.encrypt(plaintext);
        String hint = cipher.mask(plaintext);

        if (credentialId != null) {
            AiCredential existing = credentialMapper.selectById(credentialId);
            if (existing != null) {
                existing.setIv(encrypted.iv());
                existing.setTag(encrypted.tag());
                existing.setPayload(encrypted.payload());
                existing.setKeyHint(hint);
                existing.setKeyVersion(masterKeyProvider.keyVersion());
                credentialMapper.updateById(existing);
                return existing.getId();
            }
        }

        AiCredential created = new AiCredential();
        created.setOwnerType(ownerType);
        created.setOwnerId(ownerId);
        created.setCipherAlgo("AES-256-GCM");
        created.setKeyVersion(masterKeyProvider.keyVersion());
        created.setIv(encrypted.iv());
        created.setTag(encrypted.tag());
        created.setPayload(encrypted.payload());
        created.setKeyHint(hint);
        credentialMapper.insert(created);
        return created.getId();
    }

    /** 解密取明文，仅用于发起上游请求。凭证不存在时返回 null。 */
    public String plaintext(Long credentialId) {
        if (credentialId == null) {
            return null;
        }
        AiCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            return null;
        }
        try {
            return cipher.decrypt(credential.getIv(), credential.getTag(), credential.getPayload());
        } catch (IllegalStateException ex) {
            log.warn("[ai-config] decrypt credential failed, id={}, cause={}", credentialId, ex.getMessage());
            return null;
        }
    }

    /** 掩码提示（如 ak-****1b2）；无凭证返回空串。 */
    public String hint(Long credentialId) {
        if (credentialId == null) {
            return "";
        }
        AiCredential credential = credentialMapper.selectById(credentialId);
        return credential == null || credential.getKeyHint() == null ? "" : credential.getKeyHint();
    }

    public void delete(Long credentialId) {
        if (credentialId != null) {
            credentialMapper.deleteById(credentialId);
        }
    }

    /** 诊断：凭证能否用当前主密钥解开。 */
    public boolean isDecryptable(Long credentialId) {
        return plaintext(credentialId) != null;
    }
}
