package com.codeinterview.question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.NeighborResp;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionDetail;
import com.codeinterview.question.vo.QuestionDetailResp;
import com.codeinterview.question.vo.QuestionTagRow;
import com.codeinterview.question.vo.TagBriefResp;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 30;
    private static final String SORT_HOT = "hot";
    private static final String SORT_LATEST = "latest";
    private static final Set<Integer> VALID_DIFFICULTIES = Set.of(1, 2, 3);

    private final QuestionMapper questionMapper;

    public PageResp<QuestionBriefResp> page(QuestionQueryReq rawQuery) {
        QuestionQueryReq query = normalize(rawQuery);
        long total = questionMapper.countQuestionPage(query);
        if (total == 0) {
            return PageResp.of(query.getPage(), query.getPageSize(), 0, List.of());
        }
        long offset = (long) (query.getPage() - 1) * query.getPageSize();
        List<QuestionBriefResp> list = questionMapper.selectQuestionPage(query, offset, query.getPageSize());
        attachTags(list);
        return PageResp.of(query.getPage(), query.getPageSize(), total, list);
    }

    public QuestionDetailResp detail(Long questionId) {
        QuestionDetail question = requirePublished(questionId);
        attachTags(question);
        Long userId = currentUserIdOrNull();
        QuestionDetailResp resp = new QuestionDetailResp();
        resp.setQuestion(question);
        resp.setMyFavorite(userId != null && questionMapper.selectFavoriteFlag(userId, questionId) != null);
        resp.setMyAssessment(userId == null ? null : questionMapper.selectLatestAssessment(userId, questionId));
        resp.setNeighbors(buildNeighbors(questionId, question.getCategoryId()));
        increaseViewCountAsync(questionId);
        return resp;
    }

    public NeighborResp neighbors(Long questionId) {
        QuestionDetail question = requirePublished(questionId);
        return buildNeighbors(questionId, question.getCategoryId());
    }

    private QuestionDetail requirePublished(Long questionId) {
        QuestionDetail question = questionMapper.selectPublishedDetail(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return question;
    }

    private NeighborResp buildNeighbors(Long questionId, Long categoryId) {
        Long prevId = questionMapper.selectPrevId(categoryId, questionId);
        Long nextId = questionMapper.selectNextId(categoryId, questionId);
        return new NeighborResp(prevId, nextId);
    }

    /**
     * 浏览计数异步自增：不阻塞详情返回，失败只打日志，不回滚详情（03 §8.2 #6）。
     */
    private void increaseViewCountAsync(Long questionId) {
        CompletableFuture.runAsync(() -> {
            try {
                questionMapper.increaseViewCount(questionId);
            } catch (Exception ex) {
                log.warn("increase view_count failed, questionId={}, cause={}", questionId, ex.getMessage());
            }
        });
    }

    private Long currentUserIdOrNull() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception ex) {
            log.debug("read login state failed: {}", ex.getMessage());
            return null;
        }
    }

    private QuestionQueryReq normalize(QuestionQueryReq rawQuery) {
        QuestionQueryReq query = rawQuery == null ? new QuestionQueryReq() : rawQuery;
        query.setPage(query.getPage() == null || query.getPage() < 1 ? DEFAULT_PAGE : query.getPage());
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(DEFAULT_PAGE_SIZE);
        } else {
            query.setPageSize(Math.min(query.getPageSize(), MAX_PAGE_SIZE));
        }
        if (query.getCategoryId() != null && query.getCategoryId() <= 0) {
            query.setCategoryId(null);
        }
        if (query.getTagId() != null && query.getTagId() <= 0) {
            query.setTagId(null);
        }
        if (query.getDifficulty() != null && !VALID_DIFFICULTIES.contains(query.getDifficulty())) {
            query.setDifficulty(null);
        }
        query.setKeyword(normalizeKeyword(query.getKeyword()));
        query.setSort(SORT_HOT.equals(query.getSort()) ? SORT_HOT : SORT_LATEST);
        return query;
    }

    /**
     * 03 §8.1：trim → 超 30 字截断 → 转义；必须先转义反斜杠再转义 % 与 _，否则会二次转义。
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            trimmed = trimmed.substring(0, MAX_KEYWORD_LENGTH);
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 标签一次 IN 批量取回后按题目分组，禁止在循环里逐题查库（N+1）。
     */
    private void attachTags(List<QuestionBriefResp> questions) {
        List<Long> questionIds = questions.stream().map(QuestionBriefResp::getId).toList();
        List<QuestionTagRow> rows = questionMapper.selectTagsByQuestionIds(questionIds);
        Map<Long, List<TagBriefResp>> tagsByQuestion = new HashMap<>();
        for (QuestionTagRow row : rows) {
            tagsByQuestion.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                    .add(new TagBriefResp(row.getTagId(), row.getTagName()));
        }
        for (QuestionBriefResp question : questions) {
            question.setTags(tagsByQuestion.getOrDefault(question.getId(), List.of()));
        }
    }

    private void attachTags(QuestionDetail question) {
        List<QuestionTagRow> rows = questionMapper.selectTagsByQuestionIds(List.of(question.getId()));
        List<TagBriefResp> tags = new ArrayList<>(rows.size());
        for (QuestionTagRow row : rows) {
            tags.add(new TagBriefResp(row.getTagId(), row.getTagName()));
        }
        question.setTags(tags);
    }
}
