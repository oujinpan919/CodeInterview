package com.codeinterview.ai.vo;

import lombok.Data;

/** 站点开关（D2）。 */
@Data
public class SiteSettingResp {

    /** false 时用户自带 Key 不参与解析链，但已存数据保留（A7）。 */
    private Boolean allowUserOwnKey;
}
