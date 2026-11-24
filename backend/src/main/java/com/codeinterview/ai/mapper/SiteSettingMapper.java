package com.codeinterview.ai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteSettingMapper {

    @Select("SELECT setting_value FROM site_setting WHERE setting_key = #{key}")
    String selectValue(@Param("key") String key);

    @Insert("""
            INSERT INTO site_setting (setting_key, setting_value, description)
            VALUES (#{key}, #{value}, #{description})
            ON DUPLICATE KEY UPDATE setting_value = #{value}
            """)
    int upsert(@Param("key") String key, @Param("value") String value,
               @Param("description") String description);
}
