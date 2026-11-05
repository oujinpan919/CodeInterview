package com.codeinterview.chat;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * AI 配额 SQL（05 §5.1）：判定只看 UPDATE 的影响行数（1 成功 / 0 触顶），禁止先查后写。
 * 统计日统一 Asia/Shanghai（由 QuotaService 传入 LocalDate）。
 */
@Mapper
public interface QuotaMapper {

    @Insert("""
            INSERT INTO user_ai_quota (user_id, stat_date, used_count) VALUES (#{userId}, #{date}, 0)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """)
    int ensureUserQuota(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Update("""
            UPDATE user_ai_quota SET used_count = used_count + 1
            WHERE user_id = #{userId} AND stat_date = #{date} AND used_count < #{limit}
            """)
    int consumeUser(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("limit") int limit);

    @Update("""
            UPDATE user_ai_quota SET used_count = used_count - 1
            WHERE user_id = #{userId} AND stat_date = #{date} AND used_count > 0
            """)
    int rollbackUser(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Insert("""
            INSERT INTO site_ai_usage (stat_date, used_count, used_tokens) VALUES (#{date}, 0, 0)
            ON DUPLICATE KEY UPDATE stat_date = stat_date
            """)
    int ensureSiteUsage(@Param("date") LocalDate date);

    @Update("""
            UPDATE site_ai_usage SET used_count = used_count + 1
            WHERE stat_date = #{date} AND used_count < #{limit}
            """)
    int consumeSite(@Param("date") LocalDate date, @Param("limit") int limit);

    @Update("""
            UPDATE site_ai_usage SET used_count = used_count - 1
            WHERE stat_date = #{date} AND used_count > 0
            """)
    int rollbackSite(@Param("date") LocalDate date);

    @Update("UPDATE site_ai_usage SET used_tokens = used_tokens + #{tokens} WHERE stat_date = #{date}")
    int addSiteTokens(@Param("date") LocalDate date, @Param("tokens") long tokens);

    @Select("SELECT used_count FROM user_ai_quota WHERE user_id = #{userId} AND stat_date = #{date}")
    Integer selectUserUsed(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT used_count FROM site_ai_usage WHERE stat_date = #{date}")
    Integer selectSiteUsed(@Param("date") LocalDate date);
}
