package com.codeinterview.chat.dto;

import java.util.List;

import lombok.Data;

/**
 * 助手请求（05 §3.2 / 04 A3.7 #25 / 08 §5）。
 *
 * <p>D6 后上下文来源从「客户端全量回传」改为「服务端按 conversationId 取最近 N 条」，
 * 但客户端仍可传 messages（用于同一条流内尚未落库的当前消息）。
 *
 * <p>{@code sessionId} 不来自请求体业务字段，而是 Controller 从 X-Session-Id 头写入，
 * 仅用于 ai_chat_log 关联。
 */
@Data
public class ChatReq {

    private List<Msg> messages;
    private Long questionId;
    private String sessionId;
    /** D2/D13：对话页下拉选中的配置 id；为空则按解析链自动选。 */
    private Long modelConfigId;
    /** D11：Prompt 模板 id；为空则用默认 system prompt。 */
    private Long templateId;
    /** D6：继续已有会话；为空则新建。 */
    private Long conversationId;
}
