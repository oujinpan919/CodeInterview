package com.codeinterview.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.codeinterview.home.vo.HomeResp;
import com.codeinterview.question.QuestionMapper;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionTagRow;
import com.codeinterview.question.vo.TagBriefResp;
import com.codeinterview.taxonomy.CategoryService;
import com.codeinterview.taxonomy.vo.CategoryResp;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int HOME_LIST_LIMIT = 6;
    private static final int CONTINUE_LEARNING_LIMIT = 3;

    private final CategoryService categoryService;
    private final HomeMapper homeMapper;
    private final QuestionMapper questionMapper;

    /**
     * 三块数据分别查询、分别降级：任一模块失败只让该字段为空并打 ERROR，不整体 500（01 F08）。
     */
    public HomeResp home() {
        HomeResp resp = new HomeResp();
        resp.setCategories(loadCategories());
        resp.setLatest(loadLatest());
        resp.setHot(loadHot());
        resp.setContinueLearning(loadContinueLearning());
        return resp;
    }

    private List<HomeResp.HomeCategory> loadCategories() {
        try {
            List<CategoryResp> categories = categoryService.listEnabled();
            List<HomeResp.HomeCategory> result = new ArrayList<>(categories.size());
            for (CategoryResp category : categories) {
                HomeResp.HomeCategory item = new HomeResp.HomeCategory();
                item.setId(category.getId());
                item.setName(category.getName());
                item.setDescription(category.getDescription());
                item.setQuestionCount(category.getQuestionCount());
                result.add(item);
            }
            return result;
        } catch (Exception ex) {
            log.error("home categories query failed", ex);
            return List.of();
        }
    }

    private List<QuestionBriefResp> loadLatest() {
        try {
            List<QuestionBriefResp> latest = homeMapper.selectLatest(HOME_LIST_LIMIT);
            attachTags(latest);
            return latest;
        } catch (Exception ex) {
            log.error("home latest query failed", ex);
            return List.of();
        }
    }

    private List<QuestionBriefResp> loadHot() {
        try {
            List<QuestionBriefResp> hot = homeMapper.selectHot(HOME_LIST_LIMIT);
            attachTags(hot);
            return hot;
        } catch (Exception ex) {
            log.error("home hot query failed", ex);
            return List.of();
        }
    }

    /**
     * 仅登录返回；游客为 null（04 A3.2）。查询失败按"该字段为空"处理，返回空列表而非 500。
     */
    private List<HomeResp.HomeContinueLearning> loadContinueLearning() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            return null;
        }
        try {
            return homeMapper.selectContinueLearning(userId, CONTINUE_LEARNING_LIMIT);
        } catch (Exception ex) {
            log.error("home continueLearning query failed", ex);
            return List.of();
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception ex) {
            log.debug("read login state failed: {}", ex.getMessage());
            return null;
        }
    }

    private void attachTags(List<QuestionBriefResp> questions) {
        if (questions.isEmpty()) {
            return;
        }
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
}
