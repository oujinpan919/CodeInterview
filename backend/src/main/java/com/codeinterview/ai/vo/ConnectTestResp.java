package com.codeinterview.ai.vo;

import java.util.List;

import lombok.Data;

/**
 * 连接测试出参（D4）。{@code suggestV1=true} 表示用户填的地址没有路径后缀，
 * 提示「忘了 /v1」——这是 B2 单表单下最高频的故障。
 */
@Data
public class ConnectTestResp {

    /** passed / passed_degraded / failed */
    private String status;
    private Boolean ok;
    private Long latencyMs;
    private List<String> models;
    private Boolean suggestV1;
    private String error;
}
