package com.codeinterview.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.SiteModelConfig;

@Mapper
public interface SiteModelConfigMapper extends BaseMapper<SiteModelConfig> {

    /** 解析链第 3/4 步：站点默认优先，其次任一启用。 */
    @Select("""
            SELECT * FROM site_model_config
            WHERE deleted = 0 AND is_enabled = 1
            ORDER BY is_site_default DESC, id ASC
            LIMIT 1
            """)
    SiteModelConfig selectActive();

    @Update("UPDATE site_model_config SET is_site_default = 0 WHERE is_site_default = 1 AND deleted = 0")
    int clearDefault();

    @Update("""
            UPDATE site_model_config SET is_site_default = 1
            WHERE id = #{id} AND deleted = 0 AND is_enabled = 1
            """)
    int setDefault(@Param("id") Long id);
}
