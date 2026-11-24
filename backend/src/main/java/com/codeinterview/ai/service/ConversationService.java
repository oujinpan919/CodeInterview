package com.codeinterview.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.ai.entity.AiConversation;
import com.codeinterview.ai.entity.AiMessage;
import com.codeinterview.ai.mapper.AiConversationMapper;
import com.codeinterview.ai.mapper.AiMessageMapper;
import com.codeinterview.ai.vo.ConversationDetailResp;
import com.codeinterview.ai.vo.ConversationResp;
import com.codeinterview.ai.vo.MessageResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.result.PageResp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 会话持久化（D6）。替换 05 §1「服务端不持久化消息正文」红线。
 *
 * <p>所有方法都按 {@code userId} 过滤——会话正文是隐私数据，
 * 越权读取比配置越权更严重。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int TITLE_MAX = 60;
    private static final int TITLE_SOURCE_MAX = 30;
    private static final int MESSAGE_PAGE_LIMIT = 500;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    @Transactional
    public Long create(Long userId, Long questionId, Long modelConfigId) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setQuestionId(questionId);
        conversation.setModelConfigId(modelConfigId);
        conversation.setMessageCount(0);
        conversation.setDeleted(0);
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    public PageResp<ConversationResp> list(Long userId, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), 50);
        int offset = (safePage - 1) * safeSize;

        List<AiConversation> rows = conversationMapper.selectPage(userId, offset, safeSize);
        int total = conversationMapper.countByUser(userId);

        List<ConversationResp> items = new ArrayList<>(rows.size());
        for (AiConversation row : rows) {
            ConversationResp resp = new ConversationResp();
            resp.setId(row.getId());
            resp.setTitle(row.getTitle());
            resp.setQuestionId(row.getQuestionId());
            resp.setMessageCount(row.getMessageCount());
            resp.setCreatedAt(row.getCreatedAt());
            resp.setUpdatedAt(row.getUpdatedAt());
            items.add(resp);
        }
        return PageResp.of(safePage, safeSize, total, items);
    }

    public ConversationDetailResp detail(Long userId, Long id) {
        AiConversation conversation = requireOwned(userId, id);
        List<AiMessage> messages = messageMapper.selectByConversation(id, MESSAGE_PAGE_LIMIT);

        ConversationDetailResp resp = new ConversationDetailResp();
        resp.setId(conversation.getId());
        resp.setTitle(conversation.getTitle());
        resp.setQuestionId(conversation.getQuestionId());
        resp.setModelConfigId(conversation.getModelConfigId());
        resp.setCreatedAt(conversation.getCreatedAt());
        resp.setUpdatedAt(conversation.getUpdatedAt());

        List<MessageResp> items = new ArrayList<>(messages.size());
        for (AiMessage message : messages) {
            MessageResp item = new MessageResp();
            item.setId(message.getId());
            item.setRole(message.getRole());
            item.setContent(message.getContent());
            item.setStatus(message.getStatus());
            item.setStatusNote(message.getStatusNote());
            item.setCreatedAt(message.getCreatedAt());
            items.add(item);
        }
        resp.setMessages(items);
        return resp;
    }

    @Transactional
    public void rename(Long userId, Long id, String title) {
        requireOwned(userId, id);
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "标题不能为空");
        }
        if (trimmed.length() > TITLE_MAX) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "标题不能超过 " + TITLE_MAX + " 字");
        }
        conversationMapper.rename(id, userId, trimmed);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        conversationMapper.softDelete(id, userId);
    }

    /** 写入一条消息；首次用户消息时顺手生成标题。 */
    @Transactional
    public void appendMessage(Long conversationId, String role, String content, int status, String statusNote) {
        if (conversationId == null || content == null || content.isEmpty()) {
            return;
        }
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setStatus(status);
        message.setStatusNote(statusNote);
        messageMapper.insert(message);
        conversationMapper.bumpMessageCount(conversationId, 1);
    }

    /** 首条用户消息生成标题（前 30 字）。 */
    @Transactional
    public void ensureTitle(Long conversationId, String firstUserContent) {
        if (conversationId == null || firstUserContent == null || firstUserContent.isBlank()) {
            return;
        }
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !"新对话".equals(conversation.getTitle())) {
            return;
        }
        String source = firstUserContent.trim().replaceAll("\\s+", " ");
        String title = source.length() <= TITLE_SOURCE_MAX
                ? source
                : source.substring(0, TITLE_SOURCE_MAX) + "…";
        conversationMapper.rename(conversationId, conversation.getUserId(), title);
    }

    /** 更新会话关联的模型配置（发给某条消息用的那个）。 */
    @Transactional
    public void touchModel(Long conversationId, Long modelConfigId) {
        if (conversationId == null) {
            return;
        }
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return;
        }
        conversation.setModelConfigId(modelConfigId);
        conversationMapper.updateById(conversation);
    }

    /** 取最近 N 条消息作为下一轮的上下文（D6 后上下文来源从客户端改为服务端）。 */
    public List<com.codeinterview.chat.dto.Msg> recentContext(Long conversationId, int limit) {
        if (conversationId == null) {
            return List.of();
        }
        List<AiMessage> rows = messageMapper.selectByConversation(conversationId, MESSAGE_PAGE_LIMIT);
        List<com.codeinterview.chat.dto.Msg> result = new ArrayList<>();
        int from = Math.max(0, rows.size() - limit);
        for (int i = from; i < rows.size(); i++) {
            AiMessage row = rows.get(i);
            if (row.getStatus() != null && row.getStatus() != AiMessage.STATUS_OK) {
                continue;
            }
            com.codeinterview.chat.dto.Msg msg = new com.codeinterview.chat.dto.Msg();
            msg.setRole(row.getRole());
            msg.setContent(row.getContent());
            result.add(msg);
        }
        return result;
    }

    public AiConversation requireOwned(Long userId, Long id) {
        AiConversation conversation = conversationMapper.selectOwned(id, userId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return conversation;
    }
}
