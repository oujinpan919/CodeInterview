package com.codeinterview.chat;

import org.springframework.stereotype.Service;

import com.codeinterview.chat.entity.AiChatLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 写 ai_chat_log（05 §5.2 计费矩阵 / §11 日志）。任何异常都不允许影响用户侧流式体验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatLogService {

    private final AiChatLogMapper aiChatLogMapper;

    public void log(AiChatLog entry) {
        try {
            aiChatLogMapper.insert(entry);
        } catch (Exception ex) {
            log.warn("[ai] write ai_chat_log failed, userId={}, status={}, cause={}",
                    entry.getUserId(), entry.getStatus(), ex.getMessage());
        }
    }
}
