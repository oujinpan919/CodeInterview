package com.codeinterview.ai.vo;

import lombok.Data;

/**
 * 当前生效模型（对话页顶部下拉用）。
 */
@Data
public class ActiveModelResp {

    private Long id;
    private String displayName;
    private String modelId;
    private String providerType;
    private Boolean isDefault;
    private Boolean isEnabled;
    /** USER_OWN / SITE_DEFAULT / GLOBAL_FALLBACK */
    private String resolvedFrom;
    private Boolean consumsesSiteQuota;
}
