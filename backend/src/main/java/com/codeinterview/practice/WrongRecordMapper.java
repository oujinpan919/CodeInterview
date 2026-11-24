package com.codeinterview.practice;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.codeinterview.practice.vo.WrongBookItemResp;
import com.codeinterview.practice.vo.WrongBookState;
import com.codeinterview.practice.vo.WrongBookSummaryResp;

/**
 * 复习条目 + 错题本视图（01 F05 / F-008 / 03 §4.2、§8.3、§8.10）。两条写入口径必须一致：
 * 1) 自评 2/3 → 单条 INSERT ... ON DUPLICATE KEY UPDATE（不先查后写，02 §5.8）；
 * 2) 自评 1 / 手动掌握 → 掌握阶梯推进（review_streak+1，1→3→7→15 天，第 4 次退出队列）。
 *
 * <p>v3.6 变更：本表从「错题本」升级为「复习条目」，所有自评过的题都有一行（自评 1 且从未答错时
 * wrong_count=0、first_wrong_at/last_wrong_at 为 NULL）。错题本降级为 resolved/scope 视图，语义不变。
 *
 * <p>SQL 里 `wrong_record.xxx` 读到的是**本次 UPDATE 之前**的旧值：MySQL 对 UPDATE / ON DUPLICATE KEY
 * UPDATE 的赋值左→右求值，因此把 due_at、interval_days 写在 review_streak 赋值之前是有意为之。
 */
@Mapper
public interface WrongRecordMapper {

    /**
     * 自评 2/3 的状态机（5 条转移里除「手动彻底移除」外全覆盖）：
     * - 无记录 → 新建：wrong_count=1、resolved=0、interval=1、due=+1 天；
     * - 在复习队列 → wrong_count+1；自评 3 把间隔重置为 1 天，自评 2 保持原间隔（不清零）；
     * - 已掌握/真掌握 → resolved=0、清空 mastered_at、review_streak 归零、重新排程。
     */
    @Insert("""
            INSERT INTO wrong_record
              (user_id, question_id, last_assess, wrong_count, first_wrong_at, last_wrong_at,
               resolved, mastered_at, due_at, interval_days, review_streak, created_at, updated_at)
            VALUES (#{userId}, #{questionId}, #{assess}, 1, #{now}, #{now},
                    0, NULL,
                    DATE_ADD(#{now}, INTERVAL 1 DAY), 1, 0, #{now}, #{now}) AS new
            ON DUPLICATE KEY UPDATE
              last_assess = new.last_assess,
              wrong_count = wrong_record.wrong_count + 1,
              last_wrong_at = new.last_wrong_at,
              resolved = 0,
              mastered_at = NULL,
              due_at = DATE_ADD(#{now}, INTERVAL CASE WHEN new.last_assess = 3 THEN 1
                                                      ELSE GREATEST(wrong_record.interval_days, 1) END DAY),
              interval_days = CASE WHEN new.last_assess = 3 THEN 1
                                   ELSE GREATEST(wrong_record.interval_days, 1) END,
              review_streak = 0,
              updated_at = new.updated_at
            """)
    int upsertWrong(@Param("userId") Long userId,
                    @Param("questionId") Long questionId,
                    @Param("assess") int assess,
                    @Param("now") LocalDateTime now);

    /**
     * 自评「掌握」：掌握阶梯 1 → 3 → 7 → 15 天，第 4 次连续掌握判为真掌握并退出复习队列。
     * 从未进过错题本的题也会建行（v3.6：所有自评过的题都进排程），此时 wrong_count=0、答错时间为 NULL。
     */
    @Insert("""
            INSERT INTO wrong_record
              (user_id, question_id, last_assess, wrong_count, first_wrong_at, last_wrong_at,
               resolved, mastered_at, due_at, interval_days, review_streak, created_at, updated_at)
            VALUES (#{userId}, #{questionId}, 1, 0, NULL, NULL,
                    1, #{now},
                    DATE_ADD(#{now}, INTERVAL 1 DAY), 1, 1, #{now}, #{now}) AS new
            ON DUPLICATE KEY UPDATE
              last_assess = 1,
              resolved = 1,
              mastered_at = #{now},
              due_at = CASE LEAST(wrong_record.review_streak + 1, 4)
                         WHEN 4 THEN NULL
                         WHEN 3 THEN DATE_ADD(#{now}, INTERVAL 7 DAY)
                         WHEN 2 THEN DATE_ADD(#{now}, INTERVAL 3 DAY)
                         ELSE DATE_ADD(#{now}, INTERVAL 1 DAY) END,
              interval_days = CASE LEAST(wrong_record.review_streak + 1, 4)
                         WHEN 4 THEN 0 WHEN 3 THEN 7 WHEN 2 THEN 3 ELSE 1 END,
              review_streak = LEAST(wrong_record.review_streak + 1, 4),
              updated_at = #{now}
            """)
    int upsertMastered(@Param("userId") Long userId,
                       @Param("questionId") Long questionId,
                       @Param("now") LocalDateTime now);

    /**
     * 手动「标记掌握」（T10）：与自评 1 共用同一套阶梯，但只更新已存在的行；
     * 影响行数 0 是正常路径（该题从未进过复习条目），由调用方区分 40300 / 40400。
     */
    @Update("""
            UPDATE wrong_record
            SET last_assess = 1,
                resolved = 1,
                mastered_at = #{now},
                due_at = CASE LEAST(review_streak + 1, 4)
                           WHEN 4 THEN NULL
                           WHEN 3 THEN DATE_ADD(#{now}, INTERVAL 7 DAY)
                           WHEN 2 THEN DATE_ADD(#{now}, INTERVAL 3 DAY)
                           ELSE DATE_ADD(#{now}, INTERVAL 1 DAY) END,
                interval_days = CASE LEAST(review_streak + 1, 4)
                           WHEN 4 THEN 0 WHEN 3 THEN 7 WHEN 2 THEN 3 ELSE 1 END,
                review_streak = LEAST(review_streak + 1, 4),
                updated_at = #{now}
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int markMastered(@Param("userId") Long userId,
                     @Param("questionId") Long questionId,
                     @Param("now") LocalDateTime now);

    /** 自评响应里回读复习状态；行不存在返回 null（真掌握退出队列后行仍在，返回 mastered 态）。 */
    @Select("""
            SELECT resolved, wrong_count, due_at, interval_days, review_streak,
                   CASE WHEN review_streak >= 4 THEN 'mastered'
                        WHEN resolved = 0 THEN 'learning'
                        ELSE 'reviewing' END AS stage
            FROM wrong_record
            WHERE user_id = #{userId} AND question_id = #{questionId}
            LIMIT 1
            """)
    WrongBookState selectState(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 错题本分视图计数（03 §8.4）：deleted=1 的题不出现，status=2 的题照常出现。
     * scope: learning（在学/resolved=0）/ reviewing（已掌握待回顾）/ mastered（真掌握）/ all。
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            WHERE w.user_id = #{userId} AND q.deleted = 0
            <choose>
              <when test='"learning".equals(scope)'> AND w.resolved = 0 </when>
              <when test='"reviewing".equals(scope)'> AND w.resolved = 1 AND w.due_at IS NOT NULL AND w.review_streak &lt; 4 </when>
              <when test='"mastered".equals(scope)'> AND w.review_streak &gt;= 4 </when>
              <when test='"reviewed".equals(scope)'> AND w.resolved = 1 </when>
            </choose>
            <if test='categoryId != null'> AND q.category_id = #{categoryId} </if>
            <if test='difficulty != null'> AND q.difficulty = #{difficulty} </if>
            </script>
            """)
    long countByScope(@Param("userId") Long userId, @Param("scope") String scope,
                      @Param("categoryId") Long categoryId, @Param("difficulty") Integer difficulty);

    @Select("""
            <script>
            SELECT w.question_id AS question_id, w.last_assess, w.wrong_count, w.last_wrong_at,
                   w.resolved, w.due_at, w.interval_days, w.review_streak,
                   CASE WHEN w.review_streak &gt;= 4 THEN 'mastered'
                        WHEN w.resolved = 0 THEN 'learning'
                        ELSE 'reviewing' END AS stage,
                   q.title, q.difficulty, q.status, c.name AS category_name,
                   (n.id IS NOT NULL) AS has_note
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            JOIN category c ON c.id = q.category_id
            LEFT JOIN question_note n ON n.user_id = w.user_id AND n.question_id = w.question_id
            WHERE w.user_id = #{userId} AND q.deleted = 0
            <choose>
              <when test='"learning".equals(scope)'> AND w.resolved = 0 </when>
              <when test='"reviewing".equals(scope)'> AND w.resolved = 1 AND w.due_at IS NOT NULL AND w.review_streak &lt; 4 </when>
              <when test='"mastered".equals(scope)'> AND w.review_streak &gt;= 4 </when>
              <when test='"reviewed".equals(scope)'> AND w.resolved = 1 </when>
            </choose>
            <if test='categoryId != null'> AND q.category_id = #{categoryId} </if>
            <if test='difficulty != null'> AND q.difficulty = #{difficulty} </if>
            <choose>
              <when test='"learning".equals(scope)'> ORDER BY w.due_at ASC, w.last_wrong_at DESC, w.id DESC </when>
              <when test='"reviewing".equals(scope)'> ORDER BY w.due_at ASC, w.id DESC </when>
              <when test='"mastered".equals(scope)'> ORDER BY w.mastered_at DESC, w.id DESC </when>
              <otherwise> ORDER BY w.updated_at DESC, w.id DESC </otherwise>
            </choose>
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<WrongBookItemResp> selectPageByScope(@Param("userId") Long userId,
                                              @Param("scope") String scope,
                                              @Param("offset") long offset,
                                              @Param("size") int size,
                                              @Param("categoryId") Long categoryId,
                                              @Param("difficulty") Integer difficulty);

    /** 三个视图角标一次取回（04 A3.4 #19），避免前端串行打三次列表接口。 */
    @Select("""
            SELECT
              COALESCE(SUM(w.resolved = 0), 0)                                                AS learning,
              COALESCE(SUM(w.resolved = 1 AND w.due_at IS NOT NULL AND w.review_streak < 4), 0) AS reviewing,
              COALESCE(SUM(w.review_streak >= 4), 0)                                          AS mastered,
              COUNT(*)                                                                        AS total
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            WHERE w.user_id = #{userId} AND q.deleted = 0
            """)
    WrongBookSummaryResp selectSummary(@Param("userId") Long userId);

    /** 该题是否存在任意用户的复习条目：用于区分「无记录 40400」与「越权 40300」。 */
    @Select("SELECT COUNT(*) FROM wrong_record WHERE question_id = #{questionId}")
    long countAnyByQuestion(@Param("questionId") Long questionId);

    /** 手动「彻底移除」：物理删除该行（03 §1）；不存在也视为成功（04 A3.4 #18）。 */
    @Delete("DELETE FROM wrong_record WHERE user_id = #{userId} AND question_id = #{questionId}")
    int delete(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
