package com.codeinterview.practice;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.util.PageUtil;
import com.codeinterview.practice.dto.SessionAdvanceReq;
import com.codeinterview.practice.dto.SessionCreateReq;
import com.codeinterview.practice.entity.PracticeSession;
import com.codeinterview.practice.vo.SessionProgressResp;
import com.codeinterview.question.QuestionMapper;
import com.codeinterview.question.QuestionService;
import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.review.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PracticeSessionService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_ABANDONED = 3;
    private static final int STATUS_COMPLETED_SKIPPED = 4;
    private static final int ACTIVE_FLAG = 1;
    private static final String ACTION_COMPLETE = "complete";
    private static final String ACTION_ABANDON = "abandon";
    private static final String MODE_TODAY = "today";
    private static final String MODE_RANDOM = "random";

    private final PracticeSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SessionProgressResp create(Long userId, SessionCreateReq req) {
        PracticeSession active = sessionMapper.selectActiveByUser(userId);
        if (active != null) {
            return progress(active, 0);
        }
        SessionCreateReq r = req == null ? new SessionCreateReq() : req;
        int size = normalizeSize(r.getSize());
        String mode = normalizeMode(r.getMode());
        List<Long> ids;
        if (MODE_TODAY.equals(mode)) {
            // 今日计划（01 F-009）：先到期复习、再新题补足；忽略筛选条件（计划本身就是全局口径）
            ids = reviewService.planQuestionIds(userId, size);
        } else {
            QuestionQueryReq query = new QuestionQueryReq();
            query.setCategoryId(r.getCategoryId());
            query.setTagId(r.getTagId());
            query.setDifficulty(r.getDifficulty());
            ids = questionMapper.selectRandomIds(questionService.normalize(query), null, size);
        }
        if (ids.isEmpty()) {
            return null;
        }
        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setStatus(STATUS_ACTIVE);
        session.setActiveFlag(ACTIVE_FLAG);
        session.setQuestionIds(toJson(ids));
        session.setTotalCount(ids.size());
        session.setCurrentIndex(0);
        session.setCompletedCount(0);
        session.setSkippedCount(0);
        try {
            sessionMapper.insertSession(session);
        } catch (DuplicateKeyException e) {
            PracticeSession existing = sessionMapper.selectActiveByUser(userId);
            return existing == null ? null : progress(existing, 0);
        }
        return progress(session, 0);
    }

    @Transactional
    public SessionProgressResp current(Long userId) {
        PracticeSession session = sessionMapper.selectActiveByUser(userId);
        if (session == null) {
            return null;
        }
        List<Long> ids = parseIds(session.getQuestionIds());
        int expectedIndex = session.getCurrentIndex();
        if (ids.isEmpty() || expectedIndex < 0 || expectedIndex >= ids.size()) {
            return finish(session, expectedIndex, STATUS_COMPLETED_SKIPPED, 0);
        }
        List<Long> validIds = sessionMapper.selectValidQuestionIds(ids.subList(expectedIndex, ids.size()));
        int skipped = 0;
        while (expectedIndex + skipped < ids.size()
                && !validIds.contains(ids.get(expectedIndex + skipped))) {
            skipped++;
        }
        session.setCurrentIndex(expectedIndex + skipped);
        session.setSkippedCount(session.getSkippedCount() + skipped);
        if (expectedIndex + skipped >= ids.size()) {
            return finish(session, expectedIndex, STATUS_COMPLETED_SKIPPED, skipped);
        }
        if (skipped > 0) {
            sessionMapper.updateProgress(session.getId(), expectedIndex, STATUS_ACTIVE, ACTIVE_FLAG,
                    session.getCurrentIndex(), session.getCompletedCount(), session.getSkippedCount());
        }
        return progress(session, skipped);
    }

    @Transactional
    public SessionProgressResp advance(Long userId, Long sessionId, SessionAdvanceReq req) {
        SessionAdvanceReq r = req == null ? new SessionAdvanceReq() : req;
        String action = r.getAction() == null || r.getAction().isBlank() ? ACTION_COMPLETE : r.getAction();
        if (!ACTION_COMPLETE.equals(action) && !ACTION_ABANDON.equals(action)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "action 只能为 complete/abandon");
        }
        if (ACTION_COMPLETE.equals(action) && r.getQuestionId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "questionId 不能为空");
        }
        PracticeSession session = sessionMapper.selectForUpdate(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (session.getStatus() != STATUS_ACTIVE) {
            return progress(session, 0);
        }
        if (ACTION_ABANDON.equals(action)) {
            return finish(session, session.getCurrentIndex(), STATUS_ABANDONED, 0);
        }
        List<Long> ids = parseIds(session.getQuestionIds());
        int expectedIndex = session.getCurrentIndex();
        if (ids.isEmpty() || expectedIndex < 0 || expectedIndex >= ids.size()) {
            return finish(session, expectedIndex, STATUS_COMPLETED_SKIPPED, 0);
        }
        // 按题去重 +1、位置只推进一次（E-07）：只有当前断点题匹配才条件 UPDATE，重复自评/乱序 id 不重复计数
        if (!r.getQuestionId().equals(ids.get(expectedIndex))) {
            return progress(session, 0);
        }
        session.setCompletedCount(session.getCompletedCount() + 1);
        session.setCurrentIndex(expectedIndex + 1);
        if (session.getCurrentIndex() >= ids.size()) {
            return finish(session, expectedIndex, STATUS_COMPLETED, 0);
        }
        sessionMapper.updateProgress(session.getId(), expectedIndex, STATUS_ACTIVE, ACTIVE_FLAG,
                session.getCurrentIndex(), session.getCompletedCount(), session.getSkippedCount());
        return progress(session, 0);
    }

    private SessionProgressResp finish(PracticeSession session, int expectedIndex, int status, int skippedNotice) {
        session.setStatus(status);
        session.setActiveFlag(null);
        sessionMapper.updateProgress(session.getId(), expectedIndex, status, null,
                session.getCurrentIndex(), session.getCompletedCount(), session.getSkippedCount());
        return progress(session, skippedNotice);
    }

    private SessionProgressResp progress(PracticeSession session, int skippedNotice) {
        SessionProgressResp resp = new SessionProgressResp();
        resp.setSessionId(session.getId());
        resp.setStatus(session.getStatus());
        resp.setPosition(session.getCurrentIndex() + 1);
        resp.setTotalCount(session.getTotalCount());
        resp.setCompletedCount(session.getCompletedCount());
        resp.setSkippedCount(session.getSkippedCount());
        resp.setSkippedNotice(skippedNotice);
        List<Long> ids = parseIds(session.getQuestionIds());
        int index = session.getCurrentIndex();
        resp.setCurrentQuestionId(session.getStatus() == STATUS_ACTIVE
                && index >= 0 && index < ids.size() ? ids.get(index) : null);
        return resp;
    }

    private int normalizeSize(Integer rawSize) {
        int size = rawSize == null ? PageUtil.DEFAULT_PAGE_SIZE : rawSize;
        if (size < 1 || size > PageUtil.MAX_PAGE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "size 只能为 1-" + PageUtil.MAX_PAGE_SIZE);
        }
        return size;
    }

    /** mode 只认 random（默认）/ today；其余 40000，不静默降级（避免「今日计划」变成随机抽题）。 */
    private String normalizeMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return MODE_RANDOM;
        }
        String mode = rawMode.trim().toLowerCase(java.util.Locale.ROOT);
        if (!MODE_RANDOM.equals(mode) && !MODE_TODAY.equals(mode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "mode 只能为 random/today");
        }
        return mode;
    }

    private List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return ids == null ? List.of() : ids;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
