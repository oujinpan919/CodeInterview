package com.codeinterview.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.AiMessage;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {

    @Select("""
            SELECT * FROM ai_message
            WHERE conversation_id = #{conversationId}
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<AiMessage> selectByConversation(@Param("conversationId") Long conversationId,
                                         @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM ai_message WHERE conversation_id = #{conversationId}")
    int countByConversation(@Param("conversationId") Long conversationId);
}
