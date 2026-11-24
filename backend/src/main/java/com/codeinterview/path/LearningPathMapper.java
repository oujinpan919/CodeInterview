package com.codeinterview.path;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.codeinterview.path.vo.PathItemResp;
import com.codeinterview.path.vo.PathResp;

/**
 * 刷题路线（03 §13）。只读：路线与题目项由 `db/migration-v4-path.sql` 落库，
 * 本期不做管理端维护（01 F-017 边界）。
 */
@Mapper
public interface LearningPathMapper {

    @Select("""
            SELECT p.id, p.name, p.description
            FROM learning_path p
            WHERE p.status = 1
            ORDER BY p.sort ASC, p.id ASC
            """)
    List<PathResp> selectPaths();

    /** 每条路线的有效题目数（只算发布态且分类启用，与详情列表口径一致）。 */
    @Select("""
            SELECT i.path_id AS id, COUNT(*) AS totalCount
            FROM learning_path_item i
            JOIN question q ON q.id = i.question_id AND q.status = 1 AND q.deleted = 0
            JOIN category c ON c.id = q.category_id AND c.status = 1
            GROUP BY i.path_id
            """)
    List<PathResp> selectTotals();

    /**
     * 每条路线已完成题数（去重）。
     * userId 为 null（游客）时 WHERE 恒不成立 → 返回空列表，由 Service 置 null。
     */
    @Select("""
            SELECT i.path_id AS id, COUNT(DISTINCT i.question_id) AS completedCount
            FROM learning_path_item i
            JOIN question q ON q.id = i.question_id AND q.status = 1 AND q.deleted = 0
            JOIN category c ON c.id = q.category_id AND c.status = 1
            WHERE EXISTS (SELECT 1 FROM practice_record pr
                          WHERE pr.user_id = #{userId} AND pr.question_id = i.question_id)
            GROUP BY i.path_id
            """)
    List<PathResp> selectCompletedCounts(@Param("userId") Long userId);

    @Select("""
            SELECT id, name, description
            FROM learning_path
            WHERE id = #{id} AND status = 1
            """)
    PathResp selectPath(@Param("id") Long id);

    /**
     * 路线内题目（ordered by sort）；done / favorite 由 practice_record / user_favorite 是否存在判定，
     * 游客 userId 为 NULL 时两者恒 false。
     */
    @Select("""
            SELECT i.question_id AS questionId, i.sort AS sort, q.title, q.difficulty,
                   c.name AS categoryName,
                   EXISTS (SELECT 1 FROM practice_record pr
                           WHERE pr.user_id = #{userId} AND pr.question_id = i.question_id) AS done,
                   EXISTS (SELECT 1 FROM user_favorite f
                           WHERE f.user_id = #{userId} AND f.question_id = i.question_id) AS favorite
            FROM learning_path_item i
            JOIN question q ON q.id = i.question_id AND q.status = 1 AND q.deleted = 0
            JOIN category c ON c.id = q.category_id AND c.status = 1
            WHERE i.path_id = #{pathId}
            ORDER BY i.sort ASC, i.id ASC
            """)
    List<PathItemResp> selectItems(@Param("pathId") Long pathId, @Param("userId") Long userId);
}
