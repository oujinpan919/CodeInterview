package com.codeinterview.favorite;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.codeinterview.favorite.vo.FavoriteQuestionResp;

@Mapper
public interface FavoriteMapper {

    /**
     * 幂等收藏（01 F06 / 02 §5.8）：唯一键冲突靠 INSERT IGNORE 消化，禁止先查后写。
     * 重复收藏影响行数为 0，同样视为成功。
     */
    @Insert("INSERT IGNORE INTO user_favorite (user_id, question_id, created_at) "
            + "VALUES (#{userId}, #{questionId}, NOW(3))")
    int insertIgnore(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 取消收藏是物理删除（03 §1：关联表不加逻辑删除，否则再收藏会撞唯一键）。 */
    @Delete("DELETE FROM user_favorite WHERE user_id = #{userId} AND question_id = #{questionId}")
    int delete(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 已逻辑删除的题不出现在收藏列表，但 user_favorite 行保留（03 §8.3）。 */
    @Select("SELECT COUNT(*) FROM user_favorite f JOIN question q ON q.id = f.question_id "
            + "WHERE f.user_id = #{userId} AND q.deleted = 0")
    long countByUser(@Param("userId") Long userId);

    @Select("""
            SELECT f.question_id AS id, f.created_at AS favorited_at, q.title, q.difficulty,
                   q.category_id, c.name AS category_name, q.view_count, q.practice_count,
                   q.published_at, q.status
            FROM user_favorite f
            JOIN question q ON q.id = f.question_id
            JOIN category c ON c.id = q.category_id
            WHERE f.user_id = #{userId} AND q.deleted = 0
            ORDER BY f.created_at DESC, f.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<FavoriteQuestionResp> selectPageByUser(@Param("userId") Long userId,
                                                @Param("offset") long offset,
                                                @Param("size") int size);
}
