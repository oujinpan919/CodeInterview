package com.codeinterview.practice;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.codeinterview.practice.entity.PracticeSession;

@Mapper
public interface PracticeSessionMapper {

    @Select("""
            SELECT id, user_id, status, active_flag, question_ids, total_count,
                   current_index, completed_count, skipped_count, created_at, updated_at
            FROM practice_session
            WHERE user_id = #{userId} AND status = 1
            LIMIT 1
            """)
    PracticeSession selectActiveByUser(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, status, active_flag, question_ids, total_count,
                   current_index, completed_count, skipped_count, created_at, updated_at
            FROM practice_session
            WHERE id = #{id}
            FOR UPDATE
            """)
    PracticeSession selectForUpdate(@Param("id") Long id);

    @Insert("""
            INSERT INTO practice_session
              (user_id, status, active_flag, question_ids, total_count,
               current_index, completed_count, skipped_count)
            VALUES
              (#{userId}, #{status}, #{activeFlag}, #{questionIds}, #{totalCount},
               #{currentIndex}, #{completedCount}, #{skippedCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSession(PracticeSession session);

    @Update("""
            UPDATE practice_session
            SET status = #{status},
                active_flag = #{activeFlag},
                current_index = #{currentIndex},
                completed_count = #{completedCount},
                skipped_count = #{skippedCount}
            WHERE id = #{id} AND status = 1 AND current_index = #{expectedIndex}
            """)
    int updateProgress(@Param("id") Long id,
                       @Param("expectedIndex") int expectedIndex,
                       @Param("status") int status,
                       @Param("activeFlag") Integer activeFlag,
                       @Param("currentIndex") int currentIndex,
                       @Param("completedCount") int completedCount,
                       @Param("skippedCount") int skippedCount);

    @Select("""
            <script>
            SELECT q.id
            FROM question q
            JOIN category c ON c.id = q.category_id
            WHERE q.status = 1 AND q.deleted = 0 AND c.status = 1
              AND q.id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> selectValidQuestionIds(@Param("ids") List<Long> ids);
}
