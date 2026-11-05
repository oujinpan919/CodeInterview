package com.codeinterview.admin.imports;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImportMapper {

    /** 导入查重（03 §8.9）：同分类下 title + content_md 完全相同 → skipped。 */
    @Select("SELECT id FROM question "
            + "WHERE category_id = #{categoryId} AND title = #{title} "
            + "AND content_md = #{contentMd} AND deleted = 0 LIMIT 1")
    Long selectDuplicateId(@Param("categoryId") Long categoryId,
                           @Param("title") String title,
                           @Param("contentMd") String contentMd);
}
