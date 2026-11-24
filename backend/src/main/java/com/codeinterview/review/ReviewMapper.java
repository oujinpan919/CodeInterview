package com.codeinterview.review;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 今日计划取数（01 F-008 / 03 §8.9）。
 *
 * <p>两块口径：
 * <ul>
 *   <li>待复习：wrong_record 中「仍在复习队列」（due_at 非空且 review_streak &lt; 4）、已到期、且题目仍发布可练；</li>
 *   <li>新题：从未自评过的发布态题，按分类均衡 + 按日轮换（同一自然日结果稳定，不抖动）。</li>
 * </ul>
 */
@Mapper
public interface ReviewMapper {

    /** 到期待复习题数（含今日内到期）。 */
    @Select("""
            SELECT COUNT(*)
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            JOIN category c ON c.id = q.category_id
            WHERE w.user_id = #{userId}
              AND w.due_at IS NOT NULL AND w.review_streak < 4
              AND w.due_at <= #{dueBefore}
              AND q.status = 1 AND q.deleted = 0 AND c.status = 1
            """)
    long countDue(@Param("userId") Long userId, @Param("dueBefore") LocalDateTime dueBefore);

    /** 逾期题数（due_at 已过，不含今日内到期）。 */
    @Select("""
            SELECT COUNT(*)
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            JOIN category c ON c.id = q.category_id
            WHERE w.user_id = #{userId}
              AND w.due_at IS NOT NULL AND w.review_streak < 4
              AND w.due_at < #{now}
              AND q.status = 1 AND q.deleted = 0 AND c.status = 1
            """)
    long countOverdue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 到期题 id，逾期最久的在前；开刷「今日计划」时优先消逾期。 */
    @Select("""
            SELECT w.question_id
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            JOIN category c ON c.id = q.category_id
            WHERE w.user_id = #{userId}
              AND w.due_at IS NOT NULL AND w.review_streak < 4
              AND w.due_at <= #{dueBefore}
              AND q.status = 1 AND q.deleted = 0 AND c.status = 1
            ORDER BY w.due_at ASC, w.question_id ASC
            LIMIT #{limit}
            """)
    List<Long> selectDueQuestionIds(@Param("userId") Long userId,
                                    @Param("dueBefore") LocalDateTime dueBefore,
                                    @Param("limit") int limit);

    /** 从未自评过的发布态题数（新题池大小）。 */
    @Select("""
            SELECT COUNT(*)
            FROM question q
            JOIN category c ON c.id = q.category_id
            WHERE q.status = 1 AND q.deleted = 0 AND c.status = 1
              AND NOT EXISTS (SELECT 1 FROM practice_record p
                              WHERE p.user_id = #{userId} AND p.question_id = q.id)
            """)
    long countNew(@Param("userId") Long userId);

    /**
     * 新题抽样：每个分类先按日轮换取 1 道，再从中按日轮换取 limit 道。
     * MD5(id + 日键) 做确定性洗牌 —— 同一天反复刷新结果一致，跨天自动换题。
     */
    @Select("""
            WITH picked AS (
                SELECT q.id AS id,
                       ROW_NUMBER() OVER (
                           PARTITION BY q.category_id
                           ORDER BY MD5(CONCAT(q.id, '-', #{dayKey}))
                       ) AS rn
                FROM question q
                JOIN category c ON c.id = q.category_id
                WHERE q.status = 1 AND q.deleted = 0 AND c.status = 1
                  AND NOT EXISTS (SELECT 1 FROM practice_record p
                                  WHERE p.user_id = #{userId} AND p.question_id = q.id)
            )
            SELECT id FROM picked
            WHERE rn = 1
            ORDER BY MD5(CONCAT(id, '-', #{dayKey}))
            LIMIT #{limit}
            """)
    List<Long> selectNewQuestionIds(@Param("userId") Long userId,
                                    @Param("dayKey") String dayKey,
                                    @Param("limit") int limit);
}
