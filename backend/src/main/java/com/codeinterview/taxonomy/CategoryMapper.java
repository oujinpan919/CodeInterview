package com.codeinterview.taxonomy;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.codeinterview.taxonomy.vo.CategoryResp;

@Mapper
public interface CategoryMapper {

    /**
     * 只返回启用分类；questionCount 只统计发布态（03 §8.1 口径），未发布不算题数。
     */
    @Select("""
            SELECT c.id, c.name, c.description, c.sort,
                   (SELECT COUNT(*) FROM question q
                     WHERE q.category_id = c.id AND q.status = 1 AND q.deleted = 0) AS question_count
            FROM category c
            WHERE c.status = 1
            ORDER BY c.sort ASC, c.id ASC
            """)
    List<CategoryResp> selectEnabledWithQuestionCount();
}
