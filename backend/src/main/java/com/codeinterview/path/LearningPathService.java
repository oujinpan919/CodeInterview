package com.codeinterview.path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.path.vo.PathDetailResp;
import com.codeinterview.path.vo.PathItemResp;
import com.codeinterview.path.vo.PathResp;
import com.codeinterview.question.QuestionMapper;
import com.codeinterview.question.vo.QuestionTagRow;
import com.codeinterview.question.vo.TagBriefResp;

import lombok.RequiredArgsConstructor;

/**
 * 刷题路线（01 F-017 / 04 C8）。
 *
 * <p>进度口径固定为「自评去重题数 ÷ 路线有效题数」——只要这道题被自评过一次就算刷过，
 * 与统计页的「累计题数」同源，不引入新的学习状态。
 */
@Service
@RequiredArgsConstructor
public class LearningPathService {

    private final LearningPathMapper learningPathMapper;
    private final QuestionMapper questionMapper;

    public List<PathResp> list(Long userId) {
        List<PathResp> paths = learningPathMapper.selectPaths();
        if (paths.isEmpty()) {
            return paths;
        }
        Map<Long, Long> totals = index(learningPathMapper.selectTotals());
        Map<Long, Long> completed = userId == null
                ? Map.of()
                : index(learningPathMapper.selectCompletedCounts(userId));
        for (PathResp path : paths) {
            path.setTotalCount(totals.getOrDefault(path.getId(), 0L));
            path.setCompletedCount(userId == null ? null : completed.getOrDefault(path.getId(), 0L));
        }
        return paths;
    }

    public PathDetailResp detail(Long pathId, Long userId) {
        PathResp path = learningPathMapper.selectPath(pathId);
        if (path == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<PathItemResp> items = learningPathMapper.selectItems(pathId, userId);
        PathDetailResp resp = new PathDetailResp();
        resp.setId(path.getId());
        resp.setName(path.getName());
        resp.setDescription(path.getDescription());
        resp.setTotalCount((long) items.size());
        long completed = items.stream().filter(item -> Boolean.TRUE.equals(item.getDone())).count();
        resp.setCompletedCount(userId == null ? null : completed);
        attachTags(items);
        resp.setItems(items);
        return resp;
    }

    /**
     * 把两种「id + 计数」结果集统一成 map：selectTotals 填 totalCount、
     * selectCompletedCounts 填 completedCount，取非 null 的那个值。
     */
    /** v3.9：标签一次 IN 批量取回后按题目分组（路线 ≤30 题，不触发 N+1）。 */
    private void attachTags(List<PathItemResp> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> questionIds = new ArrayList<>(items.size());
        for (PathItemResp item : items) {
            questionIds.add(item.getQuestionId());
        }
        List<QuestionTagRow> rows = questionMapper.selectTagsByQuestionIds(questionIds);
        Map<Long, List<TagBriefResp>> tagsByQuestion = new HashMap<>(rows.size() * 2);
        for (QuestionTagRow row : rows) {
            tagsByQuestion.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                    .add(new TagBriefResp(row.getTagId(), row.getTagName()));
        }
        for (PathItemResp item : items) {
            item.setTags(tagsByQuestion.getOrDefault(item.getQuestionId(), List.of()));
        }
    }

    private Map<Long, Long> index(List<PathResp> rows) {
        Map<Long, Long> map = new HashMap<>(rows.size() * 2);
        for (PathResp row : rows) {
            if (row.getId() == null) {
                continue;
            }
            Long value = row.getCompletedCount() != null ? row.getCompletedCount() : row.getTotalCount();
            map.put(row.getId(), value == null ? 0L : value);
        }
        return map;
    }
}
