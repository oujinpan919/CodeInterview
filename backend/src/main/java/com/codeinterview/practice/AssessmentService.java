package com.codeinterview.practice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.practice.dto.AssessmentReq;
import com.codeinterview.practice.entity.PracticeRecord;
import com.codeinterview.practice.vo.AssessmentResp;
import com.codeinterview.practice.vo.WrongBookState;
import com.codeinterview.question.QuestionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 三档自评（01 F04 / 04 A3.4 #14）。
 * 02 §5.8：写 practice_record + 维护 wrong_record + practice_count+1 必须在同一个事务内；
 * 计数用 SQL 原子自增，错题用 INSERT ... ON DUPLICATE KEY UPDATE，均不做「先查后写」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final PracticeRecordMapper practiceRecordMapper;
    private final WrongRecordMapper wrongRecordMapper;
    private final QuestionMapper questionMapper;

    @Transactional
    public AssessmentResp assess(Long userId, Long questionId, AssessmentReq req) {
        int selfAssess = normalizeAssess(req.getSelfAssess());
        // 仅发布态可自评（01 F04）；草稿/下架/已删除统一 40400，不暴露存在性
        if (questionMapper.selectPublishedDetail(questionId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int source = normalizeSource(req.getSource());
        LocalDateTime now = LocalDateTime.now(SHANGHAI);

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setQuestionId(questionId);
        record.setSelfAssess(selfAssess);
        record.setSource(source);
        record.setCreatedAt(now);
        practiceRecordMapper.insert(record);

        // 状态机（01 §8.3）：自评 1 = 掌握（无记录则不新建）；自评 2/3 = 进本或更新
        if (selfAssess == PracticeRecord.ASSESS_MASTERED) {
            wrongRecordMapper.markMastered(userId, questionId, now);
        } else {
            wrongRecordMapper.upsertWrong(userId, questionId, selfAssess, now);
        }

        questionMapper.increasePracticeCount(questionId);

        AssessmentResp resp = new AssessmentResp();
        resp.setPracticeId(record.getId());
        WrongBookState state = wrongRecordMapper.selectState(userId, questionId);
        resp.setWrongBook(state);
        resp.setTodayCount(practiceRecordMapper.countToday(userId,
                LocalDate.now(SHANGHAI).atStartOfDay()));
        return resp;
    }

    private int normalizeAssess(Integer selfAssess) {
        if (selfAssess == null || selfAssess < PracticeRecord.ASSESS_MASTERED
                || selfAssess > PracticeRecord.ASSESS_UNKNOWN) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "selfAssess 只能为 1/2/3");
        }
        return selfAssess;
    }

    /** source 只认 1 详情 / 3 错题本复习；传 2 或未传都按 1 处理（04 A3.4 #14）。 */
    private int normalizeSource(Integer source) {
        return source != null && source == PracticeRecord.SOURCE_WRONG_BOOK
                ? PracticeRecord.SOURCE_WRONG_BOOK
                : PracticeRecord.SOURCE_DETAIL;
    }
}
