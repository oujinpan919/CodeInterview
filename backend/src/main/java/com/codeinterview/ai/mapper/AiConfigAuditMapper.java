package com.codeinterview.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.AiConfigAudit;

@Mapper
public interface AiConfigAuditMapper extends BaseMapper<AiConfigAudit> {

    @Select("SELECT COUNT(*) FROM ai_config_audit WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);
}
