package com.codeinterview.chat;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.codeinterview.chat.entity.AiChatLog;

/** ai_chat_log 写入（05 §5 / §11）：不含消息正文。 */
@Mapper
public interface AiChatLogMapper {

    @Insert("""
            INSERT INTO ai_chat_log
                (user_id, session_id, question_id, model, prompt_tokens, completion_tokens, total_tokens,
                 estimated, first_token_ms, latency_ms, status, error_code, upstream_code)
            VALUES
                (#{userId}, #{sessionId}, #{questionId}, #{model}, #{promptTokens}, #{completionTokens}, #{totalTokens},
                 #{estimated}, #{firstTokenMs}, #{latencyMs}, #{status}, #{errorCode}, #{upstreamCode})
            """)
    int insert(AiChatLog log);
}
