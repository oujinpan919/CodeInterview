package com.codeinterview.practice;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.practice.entity.PracticeRecord;
import com.codeinterview.practice.vo.PracticeRecordResp;

@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {

    /** 今日已刷数：按 Asia/Shanghai 当日 00:00 起算（03 §4.3）。 */
    @Select("SELECT COUNT(*) FROM practice_record WHERE user_id = #{userId} AND created_at >= #{todayStart}")
    long countToday(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

    /** 已逻辑删除的题不进入记录列表展示，但记录行保留（01 §3「已删除」口径）。 */
    @Select("SELECT COUNT(*) FROM practice_record pr "
            + "JOIN question q ON q.id = pr.question_id "
            + "WHERE pr.user_id = #{userId} AND q.deleted = 0")
    long countByUser(@Param("userId") Long userId);

    @Select("""
            SELECT pr.id, pr.question_id, q.title, q.difficulty, c.name AS category_name,
                   pr.self_assess, pr.source, pr.created_at
            FROM practice_record pr
            JOIN question q ON q.id = pr.question_id
            JOIN category c ON c.id = q.category_id
            WHERE pr.user_id = #{userId} AND q.deleted = 0
            ORDER BY pr.created_at DESC, pr.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<PracticeRecordResp> selectPage(@Param("userId") Long userId,
                                        @Param("offset") long offset,
                                        @Param("size") int size);

    @Select("SELECT COUNT(*) FROM practice_record pr "
            + "JOIN question q ON q.id = pr.question_id "
            + "WHERE pr.user_id = #{userId} AND q.deleted = 0 AND pr.self_assess = #{selfAssess}")
    long countByAssess(@Param("userId") Long userId, @Param("selfAssess") int selfAssess);

    @Select("""
            SELECT pr.id, pr.question_id, q.title, q.difficulty, c.name AS category_name,
                   pr.self_assess, pr.source, pr.created_at
            FROM practice_record pr
            JOIN question q ON q.id = pr.question_id
            JOIN category c ON c.id = q.category_id
            WHERE pr.user_id = #{userId} AND q.deleted = 0 AND pr.self_assess = #{selfAssess}
            ORDER BY pr.created_at DESC, pr.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<PracticeRecordResp> selectPageByAssess(@Param("userId") Long userId,
                                                @Param("selfAssess") int selfAssess,
                                                @Param("offset") long offset,
                                                @Param("size") int size);
}
