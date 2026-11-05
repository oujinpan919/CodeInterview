package com.codeinterview.practice;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
}
