package com.codeinterview.ai.dto;

import lombok.Data;

/**
 * 连接测试请求（D4）。未保存的草稿也要能测，所以凭证与地址都从请求体带。
 * {@code configId} 非空时，{@code apiKey} 留空可复用已存凭证。
 */
@Data
public class ConnectTestReq {

    private Long configId;
    private String providerType;
    private String baseUrl;
    private String apiKey;
    private String modelId;
    private Integer timeoutMs;
}
