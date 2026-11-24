package com.codeinterview.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.UserModelConfig;

@Mapper
public interface UserModelConfigMapper extends BaseMapper<UserModelConfig> {

    /** 解析链第 1/2 步：用户自己的配置（默认优先，其次任一启用；无凭证的不自动选中）。 */
    @Select("""
            SELECT * FROM user_model_config
            WHERE user_id = #{userId} AND deleted = 0 AND is_enabled = 1
              AND credential_ref IS NOT NULL
            ORDER BY is_default DESC, id ASC
            LIMIT 1
            """)
    UserModelConfig selectActive(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM user_model_config
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY is_default DESC, id ASC
            """)
    List<UserModelConfig> selectAll(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*) FROM user_model_config
            WHERE user_id = #{userId} AND deleted = 0 AND is_enabled = 1
            """)
    int countEnabled(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM user_model_config
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    UserModelConfig selectOwned(@Param("id") Long id, @Param("userId") Long userId);

    /** 设为默认前清掉其他默认（同一事务内调用）。 */
    @Update("UPDATE user_model_config SET is_default = 0 WHERE user_id = #{userId} AND is_default = 1 AND deleted = 0")
    int clearDefault(@Param("userId") Long userId);

    @Update("""
            UPDATE user_model_config SET is_default = 1
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0 AND is_enabled = 1
            """)
    int setDefault(@Param("id") Long id, @Param("userId") Long userId);

    /** D14：被会话引用时只允许停用，不允许删除。 */
    @Select("SELECT COUNT(*) FROM ai_conversation WHERE model_config_id = #{id} AND deleted = 0")
    int countReferenced(@Param("id") Long id);
}
