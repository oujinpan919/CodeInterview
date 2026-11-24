package com.codeinterview.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.AiPromptTemplate;

@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {

    @Select("SELECT * FROM ai_prompt_template WHERE enabled = 1 ORDER BY sort ASC, id ASC")
    List<AiPromptTemplate> selectEnabled();

    @Select("SELECT * FROM ai_prompt_template ORDER BY sort ASC, id ASC")
    List<AiPromptTemplate> selectAll();
}
