package com.codeinterview.taxonomy;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.codeinterview.taxonomy.vo.TagResp;

@Mapper
public interface TagMapper {

    /**
     * questionCount 只统计发布态；为 0 的标签仍然返回（04 A3.6 #23）。
     */
    @Select("""
            SELECT t.id, t.name,
                   (SELECT COUNT(*) FROM question_tag qt
                      JOIN question q ON q.id = qt.question_id
                     WHERE qt.tag_id = t.id AND q.status = 1 AND q.deleted = 0) AS question_count
            FROM tag t
            ORDER BY t.id ASC
            """)
    List<TagResp> selectWithPublishedCount();
}
