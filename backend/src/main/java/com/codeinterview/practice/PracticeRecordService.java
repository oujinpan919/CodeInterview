package com.codeinterview.practice;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.practice.vo.PracticeRecordResp;

import lombok.RequiredArgsConstructor;

/**
 * 刷题记录列表（04 A3.4 #15）。筛选条件用两条固定 SQL 表达，避免 `#{x} IS NULL OR` 破坏索引。
 */
@Service
@RequiredArgsConstructor
public class PracticeRecordService {

    private final PracticeRecordMapper practiceRecordMapper;

    public PageResp<PracticeRecordResp> page(Long userId, int page, int pageSize, Integer rawSelfAssess) {
        Integer selfAssess = (rawSelfAssess != null && rawSelfAssess >= 1 && rawSelfAssess <= 3)
                ? rawSelfAssess
                : null;
        long total = selfAssess == null
                ? practiceRecordMapper.countByUser(userId)
                : practiceRecordMapper.countByAssess(userId, selfAssess);
        if (total == 0) {
            return PageResp.of(page, pageSize, 0, List.of());
        }
        long offset = (long) (page - 1) * pageSize;
        List<PracticeRecordResp> list = selfAssess == null
                ? practiceRecordMapper.selectPage(userId, offset, pageSize)
                : practiceRecordMapper.selectPageByAssess(userId, selfAssess, offset, pageSize);
        return PageResp.of(page, pageSize, total, list);
    }
}
