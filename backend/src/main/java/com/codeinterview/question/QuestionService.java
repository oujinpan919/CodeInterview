package com.codeinterview.question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.util.PageUtil;
import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.NeighborIds;
import com.codeinterview.question.vo.NeighborResp;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionDetail;
import com.codeinterview.question.vo.QuestionDetailResp;
import com.codeinterview.question.vo.QuestionTagRow;
import com.codeinterview.question.vo.RandomQuestionResp;
import com.codeinterview.question.vo.RecentViewResp;
import com.codeinterview.question.vo.TagBriefResp;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final int MAX_KEYWORD_LENGTH = 30;
    private static final String SORT_HOT = "hot";
    private static final String SORT_LATEST = "latest";
    /** v3.8：按难度升序（易 → 难），对齐题目列表页表头排序 */
    private static final String SORT_DIFFICULTY = "difficulty";
    private static final Set<Integer> VALID_DIFFICULTIES = Set.of(1, 2, 3);
    /** v3.6 修1：上下题来源上下文 */
    private static final String CTX_LIST = "list";
    private static final String CTX_FAVORITE = "favorite";
    private static final String CTX_WRONG = "wrong";
    private static final String CTX_SCOPE_DEFAULT = "learning";
    private static final Set<String> VALID_WRONG_SCOPES =
            Set.of("learning", "reviewing", "mastered", "reviewed", "all");
    /** v3.6 C10：相关题条数 */
    private static final int SIMILAR_DEFAULT_LIMIT = 3;
    private static final int SIMILAR_MAX_LIMIT = 10;
    /** v4.2：练习页左侧「最近浏览」列表条数 */
    private static final int RECENT_VIEWS_DEFAULT_LIMIT = 50;
    private static final int RECENT_VIEWS_MAX_LIMIT = 200;

    private final QuestionMapper questionMapper;

    public PageResp<QuestionBriefResp> page(QuestionQueryReq rawQuery) {
        QuestionQueryReq query = normalize(rawQuery);
        long total = questionMapper.countQuestionPage(query);
        if (total == 0) {
            return PageResp.of(query.getPage(), query.getPageSize(), 0, List.of());
        }
        long offset = (long) (query.getPage() - 1) * query.getPageSize();
        List<QuestionBriefResp> list = questionMapper.selectQuestionPage(query, currentUserIdOrNull(),
                offset, query.getPageSize());
        attachTags(list);
        return PageResp.of(query.getPage(), query.getPageSize(), total, list);
    }

    public QuestionDetailResp detail(Long questionId) {
        return detail(questionId, null, null, null);
    }

    /**
     * 题目详情（v3.6 修1）：上下题可按来源列表上下文计算。
     *
     * @param ctx       列表上下文：list（题库列表/首页最新热门）/ favorite（我的收藏）/ wrong（错题本）；
     *                  其他值或缺省 = 同分类按 id（v3.5 行为）
     * @param scope     仅 ctx=wrong 生效，语义同 GET /wrong-book 的 scope
     * @param ctxQuery  ctx=list 时的筛选与排序（与 GET /questions 同源）
     */
    public QuestionDetailResp detail(Long questionId, String ctx, String scope, QuestionQueryReq ctxQuery) {
        QuestionDetail question = requirePublished(questionId);
        attachTags(question);
        Long userId = currentUserIdOrNull();
        QuestionDetailResp resp = new QuestionDetailResp();
        resp.setQuestion(question);
        resp.setMyFavorite(userId != null && questionMapper.selectFavoriteFlag(userId, questionId) != null);
        resp.setMyAssessment(userId == null ? null : questionMapper.selectLatestAssessment(userId, questionId));
        resp.setNeighbors(buildContextNeighbors(questionId, question.getCategoryId(), ctx, scope, ctxQuery));
        increaseViewCountAsync(questionId);
        recordQuestionViewAsync(questionId, userId);
        return resp;
    }

    /**
     * v4.1：异步记录「当前用户打开过这道题」，供题库列表渲染已读标记。
     * 与 increaseViewCountAsync 一样不阻塞详情返回 —— 已读标记晚一拍出现在列表里可以接受，
     * 但打开详情页失败不能因为写浏览记录而失败。
     */
    private void recordQuestionViewAsync(Long questionId, Long userId) {
        if (userId == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                questionMapper.upsertQuestionView(userId, questionId);
            } catch (Exception ex) {
                log.warn("record question view failed, userId={}, questionId={}, cause={}",
                        userId, questionId, ex.getMessage());
            }
        });
    }

    /**
     * 按来源列表顺序取上下题（v3.6 修1）。任何异常都回退到「同分类按 id」，
     * 因为上下题是辅助功能，不能因为它失败让详情页 500。
     */
    private NeighborResp buildContextNeighbors(Long questionId, Long categoryId,
                                               String rawCtx, String rawScope, QuestionQueryReq rawQuery) {
        String ctx = rawCtx == null ? "" : rawCtx.trim().toLowerCase(Locale.ROOT);
        try {
            if (CTX_LIST.equals(ctx)) {
                return toNeighborResp(questionMapper.selectListNeighborIds(normalize(rawQuery), questionId));
            }
            Long userId = currentUserIdOrNull();
            if (userId != null && CTX_FAVORITE.equals(ctx)) {
                return toNeighborResp(questionMapper.selectFavoriteNeighborIds(userId, questionId));
            }
            if (userId != null && CTX_WRONG.equals(ctx)) {
                return toNeighborResp(
                        questionMapper.selectWrongNeighborIds(userId, normalizeScope(rawScope), questionId));
            }
        } catch (Exception ex) {
            log.warn("context neighbors failed, questionId={}, ctx={}, cause={}",
                    questionId, ctx, ex.getMessage());
        }
        return buildNeighbors(questionId, categoryId);
    }

    private NeighborResp toNeighborResp(NeighborIds ids) {
        if (ids == null) {
            return new NeighborResp(null, null);
        }
        return new NeighborResp(ids.getPrevId(), ids.getNextId());
    }

    private String normalizeScope(String rawScope) {
        if (rawScope == null || rawScope.isBlank()) {
            return CTX_SCOPE_DEFAULT;
        }
        String scope = rawScope.trim().toLowerCase(Locale.ROOT);
        return VALID_WRONG_SCOPES.contains(scope) ? scope : CTX_SCOPE_DEFAULT;
    }

    public NeighborResp neighbors(Long questionId) {
        QuestionDetail question = requirePublished(questionId);
        return buildNeighbors(questionId, question.getCategoryId());
    }

    /**
     * v3.6 C10：相似题（同分类优先 + 共享标签），用于详情页底部「相关题」。
     * 已下架/删除/所属分类停用的一律不返回。
     */
    public List<QuestionBriefResp> similar(Long questionId, Integer rawLimit) {
        QuestionDetail question = requirePublished(questionId);
        int limit = rawLimit == null ? SIMILAR_DEFAULT_LIMIT : rawLimit;
        if (limit < 1 || limit > SIMILAR_MAX_LIMIT) {
            limit = SIMILAR_DEFAULT_LIMIT;
        }
        List<QuestionBriefResp> list =
                questionMapper.selectSimilar(questionId, question.getCategoryId(), limit);
        attachTags(list);
        return list;
    }

    public RandomQuestionResp random(QuestionQueryReq rawQuery, String excludeIds) {
        QuestionQueryReq query = rawQuery == null ? new QuestionQueryReq() : rawQuery;
        query.setKeyword(null);
        query = normalize(query);
        List<Long> ids = questionMapper.selectRandomIds(query, parseExcludeIds(excludeIds), 1);
        if (ids.isEmpty()) {
            return null;
        }
        RandomQuestionResp resp = new RandomQuestionResp();
        resp.setId(ids.get(0));
        return resp;
    }

    private List<Long> parseExcludeIds(String excludeIds) {
        if (excludeIds == null || excludeIds.isBlank()) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        for (String part : excludeIds.split(",")) {
            if (ids.size() >= 50) {
                break;
            }
            try {
                ids.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /**
     * v4.2：最近浏览的题目（题目练习页左侧列表）。
     * 未登录返回空列表 —— 浏览记录是按账号存的，游客没有可展示的历史。
     *
     * @param rawLimit 条数上限，默认 50，夹在 1–200
     */
    public List<RecentViewResp> recentViews(Integer rawLimit, Long categoryId) {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            return List.of();
        }
        int limit = rawLimit == null ? RECENT_VIEWS_DEFAULT_LIMIT : rawLimit;
        if (limit < 1 || limit > RECENT_VIEWS_MAX_LIMIT) {
            limit = RECENT_VIEWS_DEFAULT_LIMIT;
        }
        Long normalizedCategory = categoryId != null && categoryId > 0 ? categoryId : null;
        return questionMapper.selectRecentViews(userId, normalizedCategory, limit);
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

    public QuestionQueryReq normalize(QuestionQueryReq rawQuery) {
        QuestionQueryReq query = rawQuery == null ? new QuestionQueryReq() : rawQuery;
        query.setPage(PageUtil.normalizePage(query.getPage()));
        query.setPageSize(PageUtil.normalizePageSize(query.getPageSize()));
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
        query.setSort(normalizeSort(query.getSort()));
        return query;
    }

    /** 排序只接受三个白名单值：latest（默认）/ hot / difficulty（易→难），其余一律回落 latest。 */
    private String normalizeSort(String rawSort) {
        if (SORT_HOT.equals(rawSort) || SORT_DIFFICULTY.equals(rawSort)) {
            return rawSort;
        }
        return SORT_LATEST;
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
        if (questions == null || questions.isEmpty()) {
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

    private void attachTags(QuestionDetail question) {
        List<QuestionTagRow> rows = questionMapper.selectTagsByQuestionIds(List.of(question.getId()));
        List<TagBriefResp> tags = new ArrayList<>(rows.size());
        for (QuestionTagRow row : rows) {
            tags.add(new TagBriefResp(row.getTagId(), row.getTagName()));
        }
        question.setTags(tags);
    }
}
