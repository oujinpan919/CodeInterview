package com.codeinterview.favorite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.favorite.vo.FavoriteQuestionResp;
import com.codeinterview.question.QuestionMapper;
import com.codeinterview.question.vo.QuestionDetail;
import com.codeinterview.question.vo.QuestionTagRow;
import com.codeinterview.question.vo.TagBriefResp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 收藏（01 F06 / 04 A3.3 #11–#13）：幂等收藏、取消物理删除、列表按收藏时间倒序。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final FavoriteMapper favoriteMapper;
    private final QuestionMapper questionMapper;

    public boolean favorite(Long userId, Long questionId) {
        requirePublishedQuestion(questionId);
        favoriteMapper.insertIgnore(userId, questionId);
        return true;
    }

    /** 取消不存在的收藏也返回成功（04 A3.3 #12）。 */
    public boolean unfavorite(Long userId, Long questionId) {
        favoriteMapper.delete(userId, questionId);
        return false;
    }

    public PageResp<FavoriteQuestionResp> page(Long userId, Integer rawPage, Integer rawPageSize) {
        int page = rawPage == null || rawPage < 1 ? DEFAULT_PAGE : rawPage;
        int pageSize = rawPageSize == null || rawPageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(rawPageSize, MAX_PAGE_SIZE);
        long total = favoriteMapper.countByUser(userId);
        if (total == 0) {
            return PageResp.of(page, pageSize, 0, List.of());
        }
        long offset = (long) (page - 1) * pageSize;
        List<FavoriteQuestionResp> list = favoriteMapper.selectPageByUser(userId, offset, pageSize);
        attachTags(list);
        return PageResp.of(page, pageSize, total, list);
    }

    /** 未发布/草稿/下架/已删除都按 40400 处理（01 §4.4：不暴露存在性）。 */
    private void requirePublishedQuestion(Long questionId) {
        QuestionDetail question = questionMapper.selectPublishedDetail(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    /**
     * 标签一次 IN 批量取回后按题目分组，禁止在循环里逐题查库（N+1）；
     * 复用 QuestionMapper 的批量查询，避免第二份 SQL。
     */
    private void attachTags(List<FavoriteQuestionResp> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<Long> questionIds = questions.stream().map(FavoriteQuestionResp::getId).toList();
        List<QuestionTagRow> rows = questionMapper.selectTagsByQuestionIds(questionIds);
        Map<Long, List<TagBriefResp>> tagsByQuestion = new HashMap<>();
        for (QuestionTagRow row : rows) {
            tagsByQuestion.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                    .add(new TagBriefResp(row.getTagId(), row.getTagName()));
        }
        for (FavoriteQuestionResp question : questions) {
            question.setTags(tagsByQuestion.getOrDefault(question.getId(), List.of()));
        }
    }
}
