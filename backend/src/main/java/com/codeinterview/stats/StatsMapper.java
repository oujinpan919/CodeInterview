package com.codeinterview.stats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.codeinterview.stats.vo.CategoryStatResp;
import com.codeinterview.stats.vo.StatsSummaryRow;
import com.codeinterview.stats.vo.TrendRow;

/**
 * 统计查询（03 §8.5–8.7）。SQL 全部落在 resources/mapper/StatsMapper.xml。
 */
@Mapper
public interface StatsMapper {

    /** 今日 / 近 7 天 / 总条数 / 掌握条数（03 §8.5）。 */
    StatsSummaryRow selectSummary(@Param("userId") Long userId,
                                  @Param("todayStart") LocalDateTime todayStart,
                                  @Param("weekStart") LocalDateTime weekStart);

    /** 累计自评题数：COUNT(DISTINCT question_id)。 */
    long countDistinctQuestions(@Param("userId") Long userId);

    /** 连续刷题天数：最近 400 天去重日期，Java 侧倒推（03 §8.5）。 */
    List<LocalDate> selectDistinctDays(@Param("userId") Long userId, @Param("limit") int limit);

    /** 近 N 天趋势（03 §8.6）：缺失日期由 Java 侧补 0。 */
    List<TrendRow> selectTrend(@Param("userId") Long userId,
                               @Param("startDateTime") LocalDateTime startDateTime);

    /** 分类分布（03 §8.7）：count 合计等于总自评条数。 */
    List<CategoryStatResp> selectCategory(@Param("userId") Long userId);
}
