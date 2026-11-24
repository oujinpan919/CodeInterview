package com.codeinterview.practice;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.util.PageUtil;
import com.codeinterview.practice.vo.WrongBookItemResp;
import com.codeinterview.practice.vo.WrongBookSummaryResp;
import com.codeinterview.question.QuestionService;
import com.codeinterview.question.dto.QuestionQueryReq;

import lombok.RequiredArgsConstructor;

/**
 * 错题本 / 复习条目列表（01 F05 / F-008，04 A3.4 #16–#19）。
 *
 * <p>v3.6：列表从「resolved 二态」扩成三态视图 —— learning 在学、reviewing 已掌握待回顾、
 * mastered 真掌握（已退出复习队列）；另留 all / reviewed 供兼容与「全部」tab。
 * 权限口径不变（01 §4.4）：访问他人记录 40300，真正无记录才 40400。
 */
@Service
@RequiredArgsConstructor
public class WrongBookService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int RESOLVED_YES = 1;
    private static final String SCOPE_LEARNING = "learning";
    private static final String SCOPE_REVIEWED = "reviewed";
    private static final Set<String> SCOPES =
            Set.of(SCOPE_LEARNING, "reviewing", "mastered", SCOPE_REVIEWED, "all");

    private final WrongRecordMapper wrongRecordMapper;
    private final QuestionService questionService;

    public PageResp<WrongBookItemResp> page(Long userId, Integer rawResolved, String rawScope,
                                            Integer rawPage, Integer rawPageSize,
                                            Long rawCategoryId, Integer rawDifficulty) {
        String scope = resolveScope(rawResolved, rawScope);
        int page = PageUtil.normalizePage(rawPage);
        int pageSize = PageUtil.normalizePageSize(rawPageSize);
        QuestionQueryReq rawQuery = new QuestionQueryReq();
        rawQuery.setCategoryId(rawCategoryId);
        rawQuery.setDifficulty(rawDifficulty);
        QuestionQueryReq query = questionService.normalize(rawQuery);
        long total = wrongRecordMapper.countByScope(userId, scope,
                query.getCategoryId(), query.getDifficulty());
        if (total == 0) {
            return PageResp.of(page, pageSize, 0, List.of());
        }
        List<WrongBookItemResp> list = wrongRecordMapper.selectPageByScope(userId, scope,
                PageUtil.offset(page, pageSize), pageSize,
                query.getCategoryId(), query.getDifficulty());
        return PageResp.of(page, pageSize, total, list);
    }

    /** 三个视图的角标数（04 A3.4 #19）。 */
    public WrongBookSummaryResp summary(Long userId) {
        return wrongRecordMapper.selectSummary(userId);
    }

    /**
     * 幂等：已掌握再调仍返回 {resolved:1}；三列同写且不改 wrong_count（04 A3.4 #17 / 03 §4.2）。
     * 与自评 1 共用掌握阶梯（v3.6），但只更新已存在的行，避免「手动掌握」变相建条目。
     */
    public int markResolved(Long userId, Long questionId) {
        int affected = wrongRecordMapper.markMastered(userId, questionId, LocalDateTime.now(SHANGHAI));
        if (affected == 0) {
            // 无该用户的行：题目为他人错题 → 40300；完全无记录 → 40400（前端刷新列表）
            if (wrongRecordMapper.countAnyByQuestion(questionId) > 0) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return RESOLVED_YES;
    }

    /** 彻底移除是物理删除；不存在也成功。属于他人的行同样不删、不报错（不泄露存在性）。 */
    public void remove(Long userId, Long questionId) {
        wrongRecordMapper.delete(userId, questionId);
    }

    /**
     * scope 优先；未传 scope 时按 v3.5 的 resolved 兼容映射
     * （resolved=1 映射到 reviewed = 已掌握，含待回顾与真掌握，避免老前端丢数据）。
     */
    private String resolveScope(Integer rawResolved, String rawScope) {
        if (rawScope != null && !rawScope.isBlank()) {
            String scope = rawScope.trim().toLowerCase(Locale.ROOT);
            if (!SCOPES.contains(scope)) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "scope 只能为 learning/reviewing/mastered/reviewed/all");
            }
            return scope;
        }
        return rawResolved != null && rawResolved == RESOLVED_YES ? SCOPE_REVIEWED : SCOPE_LEARNING;
    }
}
