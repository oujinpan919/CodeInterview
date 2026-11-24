package com.codeinterview.auth.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 04 A3.1：当前用户仅返回 id/nickname/role（无 username 字段）。
 * v4.10：补 createdAt / lastLoginAt，供个人中心展示「加入时间 / 最近登录」。
 * 这两个字段是只读元信息，不参与登录鉴权，缺失时前端降级为不展示。
 * v4.12：补 avatar（OSS 图片地址），null 时前端回退为首字母色块。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResp {

    private String id;

    private String nickname;

    /** v4.12 头像 URL；未设置时为 null */
    private String avatar;

    private Integer role;

    /** 注册时间；Jackson 按 yyyy-MM-dd HH:mm:ss 序列化（见 JacksonConfig） */
    private LocalDateTime createdAt;

    /** 最近一次登录时间；从未登录时为 null */
    private LocalDateTime lastLoginAt;
}
