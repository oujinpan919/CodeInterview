package com.codeinterview.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.ai.entity.AiConversation;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    @Select("""
            SELECT * FROM ai_conversation
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AiConversation> selectPage(@Param("userId") Long userId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM ai_conversation WHERE user_id = #{userId} AND deleted = 0")
    int countByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM ai_conversation WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    AiConversation selectOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE ai_conversation SET title = #{title} WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int rename(@Param("id") Long id, @Param("userId") Long userId, @Param("title") String title);

    @Update("UPDATE ai_conversation SET deleted = 1 WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int softDelete(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            UPDATE ai_conversation
            SET message_count = message_count + #{delta}, updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
            """)
    int bumpMessageCount(@Param("id") Long id, @Param("delta") int delta);
}
