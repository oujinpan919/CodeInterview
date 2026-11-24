package com.codeinterview.chat;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.codeinterview.chat.entity.AiChatLog;

/** ai_chat_log 写入（05 §5 / §11 / migration-v5 §8）：不含消息正文。 */
@Mapper
public interface AiChatLogMapper {

    @Insert("""
            INSERT INTO ai_chat_log
                (user_id, session_id, question_id, model, prompt_tokens, completion_tokens, total_tokens,
                 estimated, first_token_ms, latency_ms, status, error_code, upstream_code,
                 conversation_id, resolved_from, provider_type, template_id)
            VALUES
                (#{userId}, #{sessionId}, #{questionId}, #{model}, #{promptTokens}, #{completionTokens}, #{totalTokens},
                 #{estimated}, #{firstTokenMs}, #{latencyMs}, #{status}, #{errorCode}, #{upstreamCode},
                 #{conversationId}, #{resolvedFrom}, #{providerType}, #{templateId})
            """)
    int insert(AiChatLog log);
}
