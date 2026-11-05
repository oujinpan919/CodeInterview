package com.codeinterview.practice;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.util.PageUtil;
import com.codeinterview.practice.vo.WrongBookItemResp;

import lombok.RequiredArgsConstructor;

/**
 * 错题本（01 F05 / 04 A3.4 #16–#18）。
 * 权限口径（01 §4.4）：A 用 path 参数访问 B 的记录 → 40300，不区分「不存在」与「不属于你」；
 * 真正的「无记录」才返回 40400（04 A3.4 #17）。
 */
@Service
@RequiredArgsConstructor
public class WrongBookService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int RESOLVED_NO = 0;
    private static final int RESOLVED_YES = 1;

    private final WrongRecordMapper wrongRecordMapper;

    public PageResp<WrongBookItemResp> page(Long userId, Integer rawResolved,
                                            Integer rawPage, Integer rawPageSize) {
        int resolved = rawResolved != null && rawResolved == RESOLVED_YES ? RESOLVED_YES : RESOLVED_NO;
        int page = PageUtil.normalizePage(rawPage);
        int pageSize = PageUtil.normalizePageSize(rawPageSize);
        long total = wrongRecordMapper.countByResolved(userId, resolved);
        if (total == 0) {
            return PageResp.of(page, pageSize, 0, List.of());
        }
        List<WrongBookItemResp> list = wrongRecordMapper.selectPageByResolved(userId, resolved,
                PageUtil.offset(page, pageSize), pageSize);
        return PageResp.of(page, pageSize, total, list);
    }

    /**
     * 幂等：已掌握再调仍返回 {resolved:1}；三列同写且不改 wrong_count（04 A3.4 #17 / 03 §4.2）。
     * 与自评 1 共用 WrongRecordMapper.markMastered，避免同一状态转移两份 SQL。
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

    /** 彻底移除是物理删除；不存在也成功。属于他人的行同样不删、不报错（不泄漏存在性）。 */
    public void remove(Long userId, Long questionId) {
        wrongRecordMapper.delete(userId, questionId);
    }
}
