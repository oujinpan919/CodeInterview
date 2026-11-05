package com.codeinterview.admin.questions;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.admin.vo.AdminQuestionDetailResp;
import com.codeinterview.admin.vo.AdminQuestionListItemResp;
import com.codeinterview.question.entity.Question;

/**
 * 管理端题目 CRUD（04 A3.9 #27–#33）。
 * 写操作复用 MyBatis-Plus BaseMapper<Question>（@TableLogic 负责逻辑删除）；
 * 读列表/详情仍需 JOIN 分类与自定义字段，用手写 SQL。
 */
@Mapper
public interface AdminQuestionMapper extends BaseMapper<Question> {

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM question q
            WHERE q.deleted = 0
            <if test="status != null"> AND q.status = #{status}</if>
            <if test="categoryId != null"> AND q.category_id = #{categoryId}</if>
            <if test="keyword != null and keyword != ''">
              AND q.title LIKE CONCAT('%', #{keyword}, '%') ESCAPE '\\\\'
            </if>
            </script>
            """)
    long countAdminPage(@Param("status") Integer status,
                        @Param("categoryId") Long categoryId,
                        @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT q.id, q.title, q.category_id, c.name AS category_name, q.difficulty,
                   q.status, q.source, q.published_at, q.updated_at
            FROM question q
            JOIN category c ON c.id = q.category_id
            WHERE q.deleted = 0
            <if test="status != null"> AND q.status = #{status}</if>
            <if test="categoryId != null"> AND q.category_id = #{categoryId}</if>
            <if test="keyword != null and keyword != ''">
              AND q.title LIKE CONCAT('%', #{keyword}, '%') ESCAPE '\\\\'
            </if>
            ORDER BY q.updated_at DESC, q.id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AdminQuestionListItemResp> selectAdminPage(@Param("status") Integer status,
                                                    @Param("categoryId") Long categoryId,
                                                    @Param("keyword") String keyword,
                                                    @Param("offset") long offset,
                                                    @Param("size") int size);

    @Select("""
            SELECT q.id, q.title, q.content_md, q.answer_md, q.category_id, q.difficulty,
                   q.source, q.status, q.published_at
            FROM question q
            WHERE q.id = #{id} AND q.deleted = 0
            """)
    AdminQuestionDetailResp selectAdminDetail(@Param("id") Long id);

    @Select("SELECT tag_id FROM question_tag WHERE question_id = #{questionId} ORDER BY tag_id ASC")
    List<Long> selectTagIds(@Param("questionId") Long questionId);

    /** 分类是否存在、是否启用（编辑停用分类旧题时允许保留原分类）。 */
    @Select("SELECT status FROM category WHERE id = #{id} LIMIT 1")
    Integer selectCategoryStatus(@Param("id") Long id);

    @Select("""
            <script>
            SELECT COUNT(*) FROM tag WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    long countExistingTags(@Param("ids") List<Long> ids);

    @Delete("DELETE FROM question_tag WHERE question_id = #{questionId}")
    int deleteTags(@Param("questionId") Long questionId);

    @Insert("""
            <script>
            INSERT INTO question_tag (question_id, tag_id) VALUES
            <foreach collection="ids" item="tagId" separator=",">
              (#{questionId}, #{tagId})
            </foreach>
            </script>
            """)
    int insertTags(@Param("questionId") Long questionId, @Param("ids") List<Long> ids);

    /** 首次发布写 published_at，重新发布不覆盖（03 §4.1）。 */
    @Update("UPDATE question SET status = 1, published_at = COALESCE(published_at, NOW(3)), "
            + "updated_at = NOW(3) WHERE id = #{id} AND deleted = 0")
    int publish(@Param("id") Long id);

    @Update("UPDATE question SET status = 2, updated_at = NOW(3) WHERE id = #{id} AND deleted = 0")
    int offline(@Param("id") Long id);
}
