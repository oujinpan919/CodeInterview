package com.codeinterview.auth.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/** 04 A3.1 注册响应：data = {"userId":"1001"}（Long 转字符串）。 */
@Data
@NoArgsConstructor
public class RegisterResp {

    private String userId;

    public RegisterResp(Long userId) {
        this.userId = userId == null ? null : String.valueOf(userId);
    }
}
