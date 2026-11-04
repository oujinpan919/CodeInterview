package com.codeinterview.home;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.codeinterview.home.vo.HomeResp;
import com.codeinterview.question.vo.QuestionBriefResp;

@Mapper
public interface HomeMapper {

    /**
     * 最新上架（03 §8.8）：只取发布态，published_at DESC, id DESC。
     */
    @Select("""
            SELECT q.id, q.title, q.difficulty, q.category_id, c.name AS category_name,
                   q.view_count, q.practice_count, q.published_at
            FROM question q
            JOIN category c ON c.id = q.category_id
            WHERE q.deleted = 0 AND q.status = 1
            ORDER BY q.published_at DESC, q.id DESC
            LIMIT #{limit}
            """)
    List<QuestionBriefResp> selectLatest(@Param("limit") int limit);

    /**
     * 热门（03 §8.8）：practice_count 全量累计口径，DESC, id DESC。
     */
    @Select("""
            SELECT q.id, q.title, q.difficulty, q.category_id, c.name AS category_name,
                   q.view_count, q.practice_count, q.published_at
            FROM question q
            JOIN category c ON c.id = q.category_id
            WHERE q.deleted = 0 AND q.status = 1
            ORDER BY q.practice_count DESC, q.id DESC
            LIMIT #{limit}
            """)
    List<QuestionBriefResp> selectHot(@Param("limit") int limit);

    /**
     * 继续刷（03 §8.8）：每道题取最近一次自评，按最近时间倒序取 3 条；用窗口函数避免 MAX(id) 假设。
     */
    @Select("""
            SELECT id, title, last_assess, last_assessed_at
            FROM (
                SELECT q.id AS id, q.title AS title, pr.self_assess AS last_assess,
                       pr.created_at AS last_assessed_at,
                       ROW_NUMBER() OVER (PARTITION BY pr.question_id
                                          ORDER BY pr.created_at DESC, pr.id DESC) AS rn
                FROM practice_record pr
                JOIN question q ON q.id = pr.question_id
                WHERE pr.user_id = #{userId} AND q.deleted = 0 AND q.status = 1
            ) latest
            WHERE rn = 1
            ORDER BY last_assessed_at DESC
            LIMIT #{limit}
            """)
    List<HomeResp.HomeContinueLearning> selectContinueLearning(@Param("userId") Long userId,
                                                               @Param("limit") int limit);
}
