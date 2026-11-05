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

/**
 * 错题本状态机（01 §8.3 / 03 §4.2）。两条写入口径必须一致：
 * 1) 自评 2/3 → 单条 INSERT ... ON DUPLICATE KEY UPDATE（禁止先查后写，02 §5.8）；
 * 2) 自评 1 / 手动掌握 → 三列一起写（last_assess=1、resolved=1、mastered_at）。
 */
@Mapper
public interface WrongRecordMapper {

    /**
     * 自评 2/3 的 5 条状态机转移（除「手动彻底移除」外全部覆盖）：
     * - 无记录 → 新建：wrong_count=1、first_wrong_at=last_wrong_at=now、resolved=0；
     * - 在错题本(0) → wrong_count+1、last_assess/last_wrong_at 更新；
     * - 已掌握(1) → resolved=0、清空 mastered_at、wrong_count+1。
     * MySQL 8.0.19+ 行别名写法（VALUES() 已废弃）。
     */
    @Insert("""
            INSERT INTO wrong_record
              (user_id, question_id, last_assess, wrong_count, first_wrong_at, last_wrong_at,
               resolved, mastered_at, created_at, updated_at)
            VALUES (#{userId}, #{questionId}, #{assess}, 1, #{now}, #{now}, 0, NULL, #{now}, #{now}) AS new
            ON DUPLICATE KEY UPDATE
              last_assess = new.last_assess,
              wrong_count = wrong_record.wrong_count + 1,
              last_wrong_at = new.last_wrong_at,
              resolved = 0,
              mastered_at = NULL,
              updated_at = new.updated_at
            """)
    int upsertWrong(@Param("userId") Long userId,
                    @Param("questionId") Long questionId,
                    @Param("assess") int assess,
                    @Param("now") LocalDateTime now);

    /**
     * 自评 1（或 T10 的手动标记掌握）：只更新已存在的行；从未进过错题本时不产生记录（01 F05）。
     * 影响行数 0 是正常路径，不是错误。
     */
    @Update("""
            UPDATE wrong_record
            SET last_assess = 1, resolved = 1, mastered_at = #{now}, updated_at = #{now}
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int markMastered(@Param("userId") Long userId,
                     @Param("questionId") Long questionId,
                     @Param("now") LocalDateTime now);

    /** 自评响应里回读错题状态；行不存在返回 null。 */
    @Select("SELECT resolved, wrong_count FROM wrong_record "
            + "WHERE user_id = #{userId} AND question_id = #{questionId} LIMIT 1")
    WrongBookState selectState(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 错题本列表总数（03 §8.4：deleted=1 的题不出现，status=2 的题照常出现）。 */
    @Select("SELECT COUNT(*) FROM wrong_record w JOIN question q ON q.id = w.question_id "
            + "WHERE w.user_id = #{userId} AND w.resolved = #{resolved} AND q.deleted = 0")
    long countByResolved(@Param("userId") Long userId, @Param("resolved") int resolved);

    @Select("""
            SELECT w.question_id, w.last_assess, w.wrong_count, w.last_wrong_at, w.resolved,
                   q.title, q.difficulty, q.status, c.name AS category_name
            FROM wrong_record w
            JOIN question q ON q.id = w.question_id
            JOIN category c ON c.id = q.category_id
            WHERE w.user_id = #{userId} AND w.resolved = #{resolved} AND q.deleted = 0
            ORDER BY w.last_wrong_at DESC, w.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<WrongBookItemResp> selectPageByResolved(@Param("userId") Long userId,
                                                 @Param("resolved") int resolved,
                                                 @Param("offset") long offset,
                                                 @Param("size") int size);

    /**
     * T10 手动「标记掌握」（03 §4.2）：三列必须一起写，且不改 wrong_count。
     * 影响行数 0 表示无该用户的行（从未进过错题本，或属于别的用户，由 Service 区分 40400/40300）。
     */
    @Update("""
            UPDATE wrong_record
            SET last_assess = 1, resolved = 1, mastered_at = #{now}, updated_at = #{now}
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    int markResolved(@Param("userId") Long userId,
                     @Param("questionId") Long questionId,
                     @Param("now") LocalDateTime now);

    /** 该题是否存在任意用户的错题行：用于区分「无记录 40400」与「越权 40300」。 */
    @Select("SELECT COUNT(*) FROM wrong_record WHERE question_id = #{questionId}")
    long countAnyByQuestion(@Param("questionId") Long questionId);

    /** 手动「彻底移除」：物理删除该行（03 §1）；不存在也视为成功（04 A3.4 #18）。 */
    @Delete("DELETE FROM wrong_record WHERE user_id = #{userId} AND question_id = #{questionId}")
    int delete(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
