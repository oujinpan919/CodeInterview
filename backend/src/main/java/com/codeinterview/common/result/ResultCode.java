package com.codeinterview.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(0, 200, "success"),
    PARAM_ERROR(40000, 400, "参数校验失败"),
    UNAUTHORIZED(40100, 401, "请先登录"),
    LOGIN_FAILED(40101, 401, "昵称或密码错误"),
    ACCOUNT_DISABLED(40102, 401, "账号已被禁用，请联系管理员"),
    FORBIDDEN(40300, 403, "没有权限执行该操作"),
    ADMIN_REQUIRED(40301, 403, "需要管理员权限"),
    NOT_FOUND(40400, 404, "内容不存在或已下架"),
    CONFLICT(40900, 409, "数据已存在，请检查后重试"),
    REFERENCE_CONFLICT(40901, 409, "该数据被引用，无法删除"),
    RATE_LIMITED(42900, 429, "操作太频繁，请稍后再试"),
    AI_USER_LIMIT(42901, 429, "今日额度已用完，请明天再试"),
    AI_SITE_LIMIT(42902, 429, "使用人数较多，请稍后再试"),
    LOGIN_LOCKED(42903, 429, "尝试次数过多，请 10 分钟后再试"),
    SERVER_ERROR(50000, 500, "服务异常，请稍后重试"),
    THIRD_PARTY_ERROR(50200, 502, "第三方服务调用失败"),
    AI_UNAVAILABLE(50201, 502, "AI 服务暂不可用"),
    AI_TIMEOUT(50202, 504, "AI 响应超时，请重试"),
    STORAGE_DISABLED(50203, 502, "图片上传未启用"),
    STORAGE_ERROR(50204, 502, "图片存储服务不可用，请稍后重试"),
    AI_CONFIG_LIMIT(40902, 409, "启用的模型数量已达上限"),
    AI_CONFIG_DISABLED(40302, 403, "管理员已关闭用户自带凭证功能");

    private final int code;
    private final int httpStatus;
    private final String message;

    ResultCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
