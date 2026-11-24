package com.codeinterview.note;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.codeinterview.note.vo.NoteResp;
import com.codeinterview.note.vo.QuestionNoteRow;

/**
 * 题目笔记（03 §12）。写入口径：空白内容 = 删除该行，不留空笔记占位。
 */
@Mapper
public interface NoteMapper {

    @Select("""
            SELECT content_md, updated_at
            FROM question_note
            WHERE user_id = #{userId} AND question_id = #{questionId}
            LIMIT 1
            """)
    NoteResp select(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 单条 upsert；唯一键 `uk_user_question` 保证每用户每题至多一行。 */
    @Insert("""
            INSERT INTO question_note (user_id, question_id, content_md, created_at, updated_at)
            VALUES (#{userId}, #{questionId}, #{contentMd}, #{now}, #{now}) AS new
            ON DUPLICATE KEY UPDATE
              content_md = new.content_md,
              updated_at = new.updated_at
            """)
    int upsert(@Param("userId") Long userId,
               @Param("questionId") Long questionId,
               @Param("contentMd") String contentMd,
               @Param("now") LocalDateTime now);

    @Delete("DELETE FROM question_note WHERE user_id = #{userId} AND question_id = #{questionId}")
    int delete(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 导出取数：按分类排序号 + 题目 id 稳定排序；已逻辑删除的题不导出，
     * 已下架（status=2）的题仍导出（用户自己写的内容不该因为下架而丢）。
     */
    @Select("""
            SELECT n.question_id, q.title, c.id AS category_id, c.name AS category_name,
                   c.sort AS category_sort, n.content_md, n.updated_at
            FROM question_note n
            JOIN question q ON q.id = n.question_id
            JOIN category c ON c.id = q.category_id
            WHERE n.user_id = #{userId} AND q.deleted = 0
            ORDER BY c.sort ASC, c.id ASC, q.id ASC
            """)
    List<QuestionNoteRow> selectForExport(@Param("userId") Long userId);

    /** 只取数量（导出接口不需要内容时的轻量路径/测试用）。 */
    @Select("SELECT COUNT(*) FROM question_note WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);
}
