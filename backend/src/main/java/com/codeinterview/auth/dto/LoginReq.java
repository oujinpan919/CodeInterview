package com.codeinterview.auth.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/** 04 A3.1 #2：登录（昵称 + 密码）。 */
@Data
public class LoginReq {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    private String password;
}
