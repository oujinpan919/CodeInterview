package com.codeinterview.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 04 A3.1 登录响应：{token, user{id,nickname,role}}。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {

    private String token;

    private UserResp user;
}
