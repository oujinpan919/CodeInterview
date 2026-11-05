package com.codeinterview.practice;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.util.PageUtil;
import com.codeinterview.practice.entity.PracticeRecord;
import com.codeinterview.practice.vo.PracticeRecordResp;

import lombok.RequiredArgsConstructor;

/**
 * 刷题记录列表（04 A3.4 #15）。筛选条件用两条固定 SQL 表达，避免 `#{x} IS NULL OR` 破坏索引。
 */
@Service
@RequiredArgsConstructor
public class PracticeRecordService {

    private final PracticeRecordMapper practiceRecordMapper;

    public PageResp<PracticeRecordResp> page(Long userId, Integer rawPage, Integer rawPageSize,
                                             Integer rawSelfAssess) {
        int page = PageUtil.normalizePage(rawPage);
        int pageSize = PageUtil.normalizePageSize(rawPageSize);
        Integer selfAssess = (rawSelfAssess != null
                && rawSelfAssess >= PracticeRecord.ASSESS_MASTERED
                && rawSelfAssess <= PracticeRecord.ASSESS_UNKNOWN)
                ? rawSelfAssess
                : null;
        long total = selfAssess == null
                ? practiceRecordMapper.countByUser(userId)
                : practiceRecordMapper.countByAssess(userId, selfAssess);
        if (total == 0) {
            return PageResp.of(page, pageSize, 0, List.of());
        }
        List<PracticeRecordResp> list = selfAssess == null
                ? practiceRecordMapper.selectPage(userId, PageUtil.offset(page, pageSize), pageSize)
                : practiceRecordMapper.selectPageByAssess(userId, selfAssess,
                        PageUtil.offset(page, pageSize), pageSize);
        return PageResp.of(page, pageSize, total, list);
    }
}
