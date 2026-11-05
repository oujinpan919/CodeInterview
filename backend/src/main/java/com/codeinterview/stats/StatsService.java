package com.codeinterview.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.codeinterview.stats.vo.CategoryStatResp;
import com.codeinterview.stats.vo.StatsOverviewResp;
import com.codeinterview.stats.vo.StatsSummaryRow;
import com.codeinterview.stats.vo.TrendPointResp;
import com.codeinterview.stats.vo.TrendRow;

import lombok.RequiredArgsConstructor;

/**
 * 统计（01 F07 / 04 A3.5 #19–#21）。只统计本人，口径固定为「自评记录，非考试判分」。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int TREND_DAYS_SHORT = 7;
    private static final int TREND_DAYS_LONG = 30;
    private static final int WEEK_DAYS = 7;
    private static final int STREAK_LOOKBACK_DAYS = 400;
    private static final int RATE_SCALE = 4;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatsMapper statsMapper;

    public StatsOverviewResp overview(Long userId) {
        LocalDate today = LocalDate.now(SHANGHAI);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.minusDays(WEEK_DAYS - 1L).atStartOfDay();

        StatsSummaryRow summary = statsMapper.selectSummary(userId, todayStart, weekStart);
        StatsOverviewResp resp = new StatsOverviewResp();
        resp.setTodayCount(summary.getTodayCount());
        resp.setWeekCount(summary.getWeekCount());
        resp.setTotalRecordCount(summary.getTotalCount());
        resp.setTotalQuestionCount(statsMapper.countDistinctQuestions(userId));
        resp.setMasteredRate(rate(summary.getMasteredCount(), summary.getTotalCount()));
        resp.setStreak(computeStreak(statsMapper.selectDistinctDays(userId, STREAK_LOOKBACK_DAYS), today));
        return resp;
    }

    /** days 只接受 7 或 30，其他值按 30；返回连续日期（缺失补 0），按日期升序（04 A3.5 #20）。 */
    public List<TrendPointResp> trend(Long userId, Integer days) {
        int length = days != null && days == TREND_DAYS_SHORT ? TREND_DAYS_SHORT : TREND_DAYS_LONG;
        LocalDate today = LocalDate.now(SHANGHAI);
        LocalDate startDate = today.minusDays(length - 1L);
        List<TrendRow> rows = statsMapper.selectTrend(userId, startDate.atStartOfDay());
        Map<LocalDate, Long> countByDay = new HashMap<>();
        for (TrendRow row : rows) {
            countByDay.put(row.getDay(), row.getCount());
        }
        List<TrendPointResp> points = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            LocalDate date = startDate.plusDays(i);
            points.add(new TrendPointResp(date.format(DATE_FORMAT), countByDay.getOrDefault(date, 0L)));
        }
        return points;
    }

    public List<CategoryStatResp> category(Long userId) {
        return statsMapper.selectCategory(userId);
    }

    private Double rate(long mastered, long total) {
        if (total <= 0) {
            return null;
        }
        return BigDecimal.valueOf(mastered)
                .divide(BigDecimal.valueOf(total), RATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 连续天数（01 F07）：按自然日倒推，今日无记录则从昨日算起，遇到空日中断。
     */
    private int computeStreak(List<LocalDate> days, LocalDate today) {
        if (days == null || days.isEmpty()) {
            return 0;
        }
        Set<LocalDate> daySet = new HashSet<>(days);
        LocalDate cursor = daySet.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (daySet.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
