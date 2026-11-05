package com.codeinterview.admin.taxonomy;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.codeinterview.admin.vo.AdminCategoryResp;
import com.codeinterview.taxonomy.entity.Category;
import com.codeinterview.taxonomy.entity.Tag;

/**
 * 管理端分类/标签维护与导入所需的查询（04 A3.9 #35–#41 / 03 §7）。
 */
@Mapper
public interface AdminTaxonomyMapper {

    /** 全部分类（含停用）；questionCount=未删除题数，publishedCount=其中发布态（04 A3.9 #35）。 */
    @Select("""
            SELECT c.id, c.name, c.description, c.sort, c.status,
                   (SELECT COUNT(*) FROM question q
                     WHERE q.category_id = c.id AND q.deleted = 0) AS question_count,
                   (SELECT COUNT(*) FROM question q
                     WHERE q.category_id = c.id AND q.deleted = 0 AND q.status = 1) AS published_count
            FROM category c
            ORDER BY c.sort ASC, c.id ASC
            """)
    List<AdminCategoryResp> selectAdminCategories();

    @Select("SELECT COUNT(*) FROM category WHERE id = #{id}")
    long countCategoryById(@Param("id") Long id);

    @Select("SELECT id FROM category WHERE name = #{name} LIMIT 1")
    Long selectCategoryIdByName(@Param("name") String name);

    @Insert("INSERT INTO category (name, description, sort, status) "
            + "VALUES (#{name}, #{description}, #{sort}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCategory(Category category);

    @Update("UPDATE category SET name = #{name}, description = #{description}, sort = #{sort}, "
            + "status = #{status}, updated_at = NOW(3) WHERE id = #{id}")
    int updateCategory(Category category);

    @Delete("DELETE FROM category WHERE id = #{id}")
    int deleteCategory(@Param("id") Long id);

    /** 引用校验口径与 GET /admin/categories 的 questionCount 一致（都按未删除题计）。 */
    @Select("SELECT COUNT(*) FROM question WHERE category_id = #{categoryId} AND deleted = 0")
    long countQuestionsByCategory(@Param("categoryId") Long categoryId);

    @Select("SELECT id FROM tag WHERE name = #{name} LIMIT 1")
    Long selectTagIdByName(@Param("name") String name);

    @Insert("INSERT INTO tag (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTag(Tag tag);

    @Update("UPDATE tag SET name = #{name} WHERE id = #{id}")
    int updateTag(Tag tag);

    @Delete("DELETE FROM tag WHERE id = #{id}")
    int deleteTag(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM tag WHERE id = #{id}")
    long countTagById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM question_tag WHERE tag_id = #{tagId}")
    long countQuestionTagRefs(@Param("tagId") Long tagId);
}
