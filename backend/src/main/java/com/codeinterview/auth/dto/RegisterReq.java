package com.codeinterview.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 04 A3.1 #1：注册只收昵称 + 密码（昵称即登录名）。 */
@Data
public class RegisterReq {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    private String password;
}
