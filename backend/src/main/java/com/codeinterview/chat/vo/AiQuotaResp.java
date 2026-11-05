package com.codeinterview.chat.vo;

import lombok.Data;

/** 助手额度（04 A3.7 #24）。 */
@Data
public class AiQuotaResp {

    private int dailyLimit;
    private int used;
    private int remaining;
    private int siteRemaining;
    private String resetAt;
}
