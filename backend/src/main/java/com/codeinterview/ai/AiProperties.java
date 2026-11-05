package com.codeinterview.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * AI 助手配置（05 §7）。base-url / model / api-key 一律配置化，禁止硬编码；
 * mock profile 只覆盖 base-url / model / api-key（见 application-mock.yml）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 总开关；false 时 /ai/chat 流前 50201。 */
    private boolean enabled = true;
    private String provider = "spark";
    private String baseUrl = "https://maas-api.cn-huabei-1.xf-yun.com/v2";
    /** 讯飞 Spark-X2.6，不要臆造别名。 */
    private String model = "spark-x2.6";
    /** 讯飞 Key 前缀是 ak-；只来自配置文件 import 或环境变量，不入库不入日志。 */
    private String apiKey = "";
    /** 固定 false：讯飞默认 thinking.type=enabled，请求体写死 disabled，不开放配置（05 §2.1）。 */
    private boolean thinkingEnabled = false;
    private int connectTimeoutMs = 5000;
    private int firstTokenTimeoutMs = 20000;
    private int idleTimeoutMs = 45000;
    private int totalTimeoutMs = 180000;
    private int maxTokens = 1200;
    private double temperature = 0.3;
    private int maxMessages = 10;
    private int maxChars = 6000;
    private int maxSingleChars = 2000;
    private String systemPromptFile = "classpath:prompt/system.txt";

    private Quota quota = new Quota();
    private Limit limit = new Limit();
    private Pool pool = new Pool();

    @Data
    public static class Quota {
        private int userDaily = 30;
        private int siteDaily = 300;
    }

    @Data
    public static class Limit {
        private int userConcurrency = 1;
        private int siteConcurrency = 4;
        private long minIntervalMs = 3000L;
    }

    @Data
    public static class Pool {
        private int coreSize = 4;
        private int maxSize = 8;
        private int queueCapacity = 20;
    }
}
