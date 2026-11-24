package com.codeinterview.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 图片对象存储（v4.12）：上传 / 删除。业务层只拿返回的 URL，不接触 OSS SDK。
 *
 * <p>三条安全约束：
 * <ul>
 *   <li><b>不信任客户端 Content-Type</b>：按文件头魔数嗅探真实类型，
 *       只放行 png/jpeg/webp/gif。客户端声明 image/png 但内容是 HTML 的一律拒绝——
 *       否则 OSS 上会存下一个可被当页面渲染的文件；</li>
 *   <li><b>扩展名由服务端决定</b>：从嗅探结果推导，不取用户文件名，
 *       杜绝 {@code ../../} 与 {@code x.html} 这类路径穿越 / 类型伪装；</li>
 *   <li><b>删除限定前缀</b>：只删 {@code prefix} 下的对象，
 *       防止越权删除同 Bucket 的其他文件（如交付截图）。</li>
 * </ul>
 */
@Slf4j
@Service
public class StorageService {

    /** 魔数嗅探所需的最长头部长度（webp 需要读到第 12 字节）。 */
    private static final int HEADER_BYTES = 12;

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties properties;

    /** 懒创建：没配 OSS 时不构造客户端，也不在启动期访问网络。 */
    private volatile OSS client;

    public StorageService(StorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 上传图片，返回可公开访问的 URL。
     *
     * @param file   浏览器上传的文件
     * @param folder 业务子目录（avatar / question / note），只允许小写字母与短横线
     * @param userId 上传者；key 内固化 owner，删除时据此做归属校验
     */
    public UploadResult upload(MultipartFile file, String folder, Long userId) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.STORAGE_DISABLED);
        }
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > properties.getMaxBytes()) {
            long mb = properties.getMaxBytes() / 1024 / 1024;
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片不能超过 " + mb + "MB");
        }

        byte[] header = readHeader(file);
        ImageType type = ImageType.sniff(header);
        if (type == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只支持 PNG / JPEG / WebP / GIF 图片");
        }

        String safeFolder = normalizeFolder(folder);
        String key = buildKey(safeFolder, type.extension, userId);

        try (InputStream in = file.getInputStream()) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(type.contentType);
            // 图片内容不可变（key 含 UUID），可长缓存；public-read 下由 OSS 直接对外服务
            meta.setCacheControl("public, max-age=31536000, immutable");
            oss().putObject(properties.getBucket(), key, in, meta);
        } catch (IOException ex) {
            log.error("[storage] read upload stream failed: key={}", key, ex);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "图片读取失败，请重试");
        } catch (RuntimeException ex) {
            log.error("[storage] put object failed: key={}", key, ex);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "图片上传失败，请重试");
        }

        String url = properties.resolvePublicBaseUrl() + "/" + key;
        log.info("[storage] uploaded key={} size={}B type={}", key, file.getSize(), type.contentType);
        return new UploadResult(key, url, file.getSize());
    }

    /**
     * 删除本服务上传的对象。调用方传 key（不是 URL），且必须落在配置的 prefix 下。
     * 删除失败只记日志不抛异常：旧图删不掉不该阻断「换头像」这类主流程。
     */
    public void deleteQuietly(String key) {
        if (!properties.isEnabled() || key == null || key.isBlank()) {
            return;
        }
        String prefix = normalizePrefix();
        if (!prefix.isEmpty() && !key.startsWith(prefix + "/")) {
            log.warn("[storage] refuse to delete object outside prefix: key={}", key);
            return;
        }
        try {
            oss().deleteObject(properties.getBucket(), key);
            log.info("[storage] deleted key={}", key);
        } catch (RuntimeException ex) {
            log.warn("[storage] delete failed, ignored: key={} cause={}", key, ex.getMessage());
        }
    }

    /** 从公网 URL 反推 key，仅用于「换头像后清理旧图」；解析不出或不在前缀下返回 null。 */
    public String keyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String base = properties.resolvePublicBaseUrl();
        if (!url.startsWith(base + "/")) {
            return null;
        }
        String key = url.substring(base.length() + 1);
        String prefix = normalizePrefix();
        if (!prefix.isEmpty() && !key.startsWith(prefix + "/")) {
            return null;
        }
        return key;
    }

    /** 判断 key 是否是某个用户上传的对象（key 格式：prefix/folder/userId/yyyy/MM/dd/uuid.ext）。 */
    public boolean isOwnedKey(Long userId, String key) {
        if (userId == null || key == null || key.isBlank()) {
            return false;
        }
        String prefix = normalizePrefix();
        String relative = key;
        if (!prefix.isEmpty()) {
            if (!key.startsWith(prefix + "/")) {
                return false;
            }
            relative = key.substring(prefix.length() + 1);
        }
        String[] parts = relative.split("/");
        return parts.length >= 2 && String.valueOf(userId).equals(parts[1]);
    }

    /** 头像 URL 是否属于该用户；用于阻止把他人公开图片设为头像再借换头像删除。 */
    public boolean isOwnedUrl(Long userId, String url) {
        return isOwnedKey(userId, keyFromUrl(url));
    }

    /** 只删除属于该用户的对象；旧格式/他人 key 一律跳过。 */
    public void deleteOwnedQuietly(Long userId, String key) {
        if (!isOwnedKey(userId, key)) {
            if (key != null && !key.isBlank()) {
                log.warn("[storage] refuse to delete non-owned object: userId={} key={}", userId, key);
            }
            return;
        }
        deleteQuietly(key);
    }

    private OSS oss() {
        OSS local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    local = new OSSClientBuilder().build(
                            properties.getEndpoint(),
                            properties.getAccessKeyId(),
                            properties.getAccessKeySecret());
                    client = local;
                }
            }
        }
        return local;
    }

    private byte[] readHeader(MultipartFile file) {
        byte[] buffer = new byte[HEADER_BYTES];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(buffer, 0, HEADER_BYTES);
            if (read < 4) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "图片内容为空或已损坏");
            }
            return buffer;
        } catch (IOException ex) {
            log.error("[storage] read header failed", ex);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "图片读取失败，请重试");
        }
    }

    private String buildKey(String folder, String extension, Long userId) {
        String prefix = normalizePrefix();
        StringBuilder key = new StringBuilder();
        if (!prefix.isEmpty()) {
            key.append(prefix).append('/');
        }
        key.append(folder).append('/').append(userId).append('/');
        if (properties.isDatePath()) {
            key.append(LocalDate.now().format(DATE_PATH)).append('/');
        }
        key.append(UUID.randomUUID().toString().replace("-", "")).append('.').append(extension);
        return key.toString();
    }

    /** 只允许小写字母与短横线，防止业务方传 ../ 或绝对路径拼进 key。 */
    private String normalizeFolder(String folder) {
        String value = folder == null ? "" : folder.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || !value.matches("[a-z-]{1,32}")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法的上传目录");
        }
        return value;
    }

    private String normalizePrefix() {
        String prefix = properties.getPrefix() == null ? "" : properties.getPrefix().trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    @PreDestroy
    public void shutdown() {
        OSS local = client;
        if (local != null) {
            local.shutdown();
        }
    }

    /** 上传结果：key 用于后续删除，url 给前端直接展示。 */
    public record UploadResult(String key, String url, long size) {
    }

    /**
     * 按魔数识别的图片类型。扩展名与 Content-Type 都由这里决定，不取用户输入。
     */
    enum ImageType {

        PNG("png", "image/png"),
        JPEG("jpg", "image/jpeg"),
        GIF("gif", "image/gif"),
        WEBP("webp", "image/webp");

        final String extension;
        final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        static ImageType sniff(byte[] h) {
            if (h == null || h.length < 4) {
                return null;
            }
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
                return PNG;
            }
            // JPEG: FF D8 FF
            if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            // GIF: "GIF87a" / "GIF89a"
            if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
                return GIF;
            }
            // WebP: "RIFF"????"WEBP"
            if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
