package com.codeinterview.chat.dto;

import java.util.List;

import lombok.Data;

/**
 * 助手请求（05 §3.2 / 04 A3.7 #25）：上下文由前端携带，服务端只校验/静默裁剪。
 * {@code sessionId} 不来自请求体业务字段，而是 Controller 从 X-Session-Id 头写入，仅用于 ai_chat_log 关联。
 */
@Data
public class ChatReq {

    private List<Msg> messages;
    private Long questionId;
    private String sessionId;
}
