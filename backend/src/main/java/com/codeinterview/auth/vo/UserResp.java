package com.codeinterview.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 04 A3.1：当前用户仅返回 id/nickname/role（无 username 字段）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResp {

    private String id;

    private String nickname;

    private Integer role;
}
