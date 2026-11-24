package com.codeinterview.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 对象存储配置（v4.12）。凭证只来自 config/application-local.yml（已 gitignore）或环境变量，
 * 与 ai.api-key 同一套约定：application.yml 里只留 ${...} 占位，绝不出现真实 Key。
 *
 * <p>enabled=false 时上传接口返回 50203，站点其余功能不受影响——
 * 本机没配 OSS 也能正常开发（头像回退到首字母色块、Markdown 图片不显示）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** 总开关；false 时 /uploads 返回 50203「图片上传未启用」。 */
    private boolean enabled = false;

    /** 阿里云 OSS Endpoint，如 oss-cn-shenzhen.aliyuncs.com。 */
    private String endpoint = "";

    private String bucket = "";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    /**
     * 对象键前缀，如 codeinterview/images。
     * 生产建议按环境分开（dev/、prod/），避免测试图混进正式目录。
     */
    private String prefix = "codeinterview/images";

    /**
     * 对外访问域名（可选）。为空时按 https://{bucket}.{endpoint} 拼；
     * 挂了 CDN 或自定义域名时填这里，前端拿到的就是该域名下的 URL。
     */
    private String publicBaseUrl = "";

    /** 单文件大小上限（字节），默认 5MB。超出直接 40000，不进内存拷贝。 */
    private long maxBytes = 5L * 1024 * 1024;

    /**
     * 图片按天分目录（yyyy/MM/dd），便于按时间清理，也避免单目录对象数过多。
     */
    private boolean datePath = true;

    /** 计算出的公网基础地址，末尾不带斜杠。 */
    public String resolvePublicBaseUrl() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (!base.isEmpty()) {
            return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        }
        String host = endpoint == null ? "" : endpoint.trim();
        if (host.startsWith("https://")) {
            host = host.substring("https://".length());
        } else if (host.startsWith("http://")) {
            host = host.substring("http://".length());
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return "https://" + bucket + "." + host;
    }
}
