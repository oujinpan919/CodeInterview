package com.codeinterview.chat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.chat.dto.ChatReq;
import com.codeinterview.chat.dto.Msg;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.question.QuestionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上下文校验 + 静默裁剪（05 §3.2「上下文语义」）。
 *
 * <p>语义是「能救就救，救不了才报错」：客户端回传超限属正常现象，默认静默裁剪后继续请求。
 * 顺序不可反：先按条数截（最近 10 条）→ 再按总字符截（6000，从最旧丢）→ 始终保留末条 user。
 * 裁剪后仍不合规（末条不是 user / 角色非法 / 单条 > 2000 等）才 40000。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatValidator {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";

    private final AiProperties props;
    private final QuestionMapper questionMapper;

    public List<Msg> validateAndTrim(ChatReq req) {
        if (req == null) {
            throw paramError("请求体不能为空");
        }
        // questionId 只查库拼 prompt：必须是发布态，否则 40400（不信任客户端文本）
        if (req.getQuestionId() != null && questionMapper.countPublished(req.getQuestionId()) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<Msg> messages = trimToBudget(normalize(req.getMessages()));
        validateAfterTrim(messages);
        return messages;
    }

    /** 角色白名单：system 丢弃（不允许覆盖服务端 system），其余非法角色 40000。 */
    private List<Msg> normalize(List<Msg> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            throw paramError("messages 不能为空");
        }
        List<Msg> messages = new ArrayList<>(rawMessages.size());
        for (Msg raw : rawMessages) {
            if (raw == null) {
                continue;
            }
            String role = raw.getRole() == null ? "" : raw.getRole().trim().toLowerCase();
            if (ROLE_SYSTEM.equals(role)) {
                continue;
            }
            if (!ROLE_USER.equals(role) && !ROLE_ASSISTANT.equals(role)) {
                throw paramError("role 只支持 user / assistant");
            }
            Msg message = new Msg();
            message.setRole(role);
            message.setContent(raw.getContent() == null ? "" : raw.getContent());
            messages.add(message);
        }
        if (messages.isEmpty()) {
            throw paramError("messages 不能为空");
        }
        return messages;
    }

    /** 静默裁剪：先按条数截最近 N 条，再按总字符从最旧丢，末条（当前用户消息）必须保留。 */
    private List<Msg> trimToBudget(List<Msg> messages) {
        List<Msg> trimmed = messages;
        int maxMessages = props.getMaxMessages();
        if (trimmed.size() > maxMessages) {
            trimmed = new ArrayList<>(trimmed.subList(trimmed.size() - maxMessages, trimmed.size()));
        }
        int maxChars = props.getMaxChars();
        while (trimmed.size() > 1 && totalChars(trimmed) > maxChars) {
            trimmed.remove(0);
        }
        return trimmed;
    }

    /** 裁剪后仍不合规才 40000。 */
    private void validateAfterTrim(List<Msg> messages) {
        Msg last = messages.get(messages.size() - 1);
        if (!ROLE_USER.equals(last.getRole())) {
            throw paramError("最后一条消息必须是用户消息");
        }
        for (Msg message : messages) {
            if (message.getContent().isBlank()) {
                throw paramError("消息内容不能为空");
            }
            if (message.getContent().length() > props.getMaxSingleChars()) {
                throw paramError("单条消息不能超过 " + props.getMaxSingleChars() + " 个字符");
            }
        }
        if (totalChars(messages) > props.getMaxChars()) {
            throw paramError("内容过长，请缩短后重试");
        }
    }

    private int totalChars(List<Msg> messages) {
        int total = 0;
        for (Msg message : messages) {
            total += message.getContent() == null ? 0 : message.getContent().length();
        }
        return total;
    }

    private BusinessException paramError(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }
}
