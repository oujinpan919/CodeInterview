package com.codeinterview.ai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.AiCredential;

@Mapper
public interface AiCredentialMapper extends BaseMapper<AiCredential> {
}
