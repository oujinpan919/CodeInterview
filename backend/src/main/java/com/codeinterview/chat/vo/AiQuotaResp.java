package com.codeinterview.chat.vo;

import lombok.Data;

/**
 * 助手额度（04 A3.7 #24）。
 *
 * <p>{@code unlimited=true} 只对管理员返回（role=1）：账号日额度不计数，前端据此显示「不限次」
 * 并跳过额度用尽禁用态。普通用户恒为 false。
 */
@Data
public class AiQuotaResp {

    private int dailyLimit;
    private int used;
    private int remaining;
    private int siteRemaining;
    private String resetAt;
    private boolean unlimited;
}
