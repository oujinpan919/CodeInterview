package com.codeinterview.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.codeinterview.review.vo.ReviewSummaryResp;

import lombok.RequiredArgsConstructor;

/**
 * 今日计划（01 F-008 / F-009）。
 *
 * <p>本服务只做「今天该刷哪几道」的取数，不产生任何写入：自评后的排程推进全部在
 * {@code AssessmentService} 的事务内完成。口径固定为 Asia/Shanghai 自然日。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int NEW_QUESTION_QUOTA = 5;
    private static final double MINUTES_PER_DUE = 0.8D;
    private static final double MINUTES_PER_NEW = 1.2D;
    private static final int MAX_PLAN_SIZE = 100;

    private final ReviewMapper reviewMapper;

    /** 今日计划摘要（首页卡片 / 刷题记录页顶部）。 */
    public ReviewSummaryResp summary(Long userId) {
        LocalDate today = LocalDate.now(SHANGHAI);
        LocalDateTime now = LocalDateTime.now(SHANGHAI);
        LocalDateTime dueBefore = endOfDay(today);
        long due = reviewMapper.countDue(userId, dueBefore);
        long overdue = reviewMapper.countOverdue(userId, now);
        long newCount = Math.min(NEW_QUESTION_QUOTA, reviewMapper.countNew(userId));

        ReviewSummaryResp resp = new ReviewSummaryResp();
        resp.setDueCount(due);
        resp.setOverdueCount(overdue);
        resp.setNewCount(newCount);
        resp.setEstimatedMinutes(estimateMinutes(due, newCount));
        return resp;
    }

    /**
     * 今日计划的题目 id 序列：先到期复习（逾期最久优先），不足 size 时用新题补足。
     * 新题不足时返回长度可能小于 size；两项都为空时返回空列表（调用方按「无可练习题目」处理）。
     */
    public List<Long> planQuestionIds(Long userId, int size) {
        int target = Math.max(1, Math.min(size, MAX_PLAN_SIZE));
        LocalDate today = LocalDate.now(SHANGHAI);
        List<Long> ids = new ArrayList<>(reviewMapper.selectDueQuestionIds(
                userId, endOfDay(today), target));
        int remaining = target - ids.size();
        if (remaining > 0) {
            ids.addAll(reviewMapper.selectNewQuestionIds(userId, dayKey(today), remaining));
        }
        return ids;
    }

    private LocalDateTime endOfDay(LocalDate day) {
        return day.plusDays(1).atStartOfDay().minusNanos(1_000_000L);
    }

    private String dayKey(LocalDate day) {
        return day.toString();
    }

    private long estimateMinutes(long dueCount, long newCount) {
        double minutes = dueCount * MINUTES_PER_DUE + newCount * MINUTES_PER_NEW;
        return minutes <= 0 ? 0 : (long) Math.ceil(minutes);
    }
}
