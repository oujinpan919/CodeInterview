package com.codeinterview.question;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.NeighborIds;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionDetail;
import com.codeinterview.question.vo.QuestionTagRow;
import com.codeinterview.question.vo.RecentViewResp;

@Mapper
public interface QuestionMapper {

    long countQuestionPage(@Param("query") QuestionQueryReq query);

    List<QuestionBriefResp> selectQuestionPage(@Param("query") QuestionQueryReq query,
                                               @Param("userId") Long userId,
                                               @Param("offset") long offset,
                                               @Param("size") int size);

    List<Long> selectRandomIds(@Param("query") QuestionQueryReq query,
                               @Param("excludeIds") List<Long> excludeIds,
                               @Param("size") int size);

    List<QuestionTagRow> selectTagsByQuestionIds(@Param("ids") List<Long> ids);

    QuestionDetail selectPublishedDetail(@Param("id") Long id);

    Long selectPrevId(@Param("categoryId") Long categoryId, @Param("id") Long id);

    Long selectNextId(@Param("categoryId") Long categoryId, @Param("id") Long id);

    /** v3.6 修1：按「题库列表」上下文（筛选 + 排序）取相邻题，SQL 见 QuestionMapper.xml。 */
    NeighborIds selectListNeighborIds(@Param("query") QuestionQueryReq query, @Param("id") Long id);

    /** v3.6 修1：按「我的收藏」顺序（收藏时间倒序）取相邻题。 */
    NeighborIds selectFavoriteNeighborIds(@Param("userId") Long userId, @Param("id") Long id);

    /** v3.6 修1：按「错题本」视图顺序取相邻题；scope 与 GET /wrong-book 同义。 */
    NeighborIds selectWrongNeighborIds(@Param("userId") Long userId,
                                       @Param("scope") String scope,
                                       @Param("id") Long id);

    Integer selectFavoriteFlag(@Param("userId") Long userId, @Param("questionId") Long questionId);

    Integer selectLatestAssessment(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Update("UPDATE question SET practice_count = practice_count + 1 WHERE id = #{id} AND deleted = 0")
    int increasePracticeCount(@Param("id") Long id);

    /** 发布态存在性判断（自评/收藏前置校验）：只查 COUNT，不拉整行。 */
    @Select("SELECT COUNT(*) FROM question WHERE id = #{id} AND status = 1 AND deleted = 0")
    int countPublished(@Param("id") Long id);

    /** v3.6 C10：相似题候选（同分类或共享标签，按同分类优先 + 热度），SQL 见 QuestionMapper.xml。 */
    List<QuestionBriefResp> selectSimilar(@Param("id") Long id,
                                          @Param("categoryId") Long categoryId,
                                          @Param("limit") int limit);

    int increaseViewCount(@Param("id") Long id);

    /**
     * v4.1：记录一次用户浏览（题库列表「已读」标记的数据源）。
     * 幂等 upsert：首次插入，重复访问只刷新 last_viewed_at。
     */
    int upsertQuestionView(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * v4.2：最近浏览的题目（题目练习页左侧列表）。
     * 只返回仍可见的题（question 未删除/已发布、分类启用），按 last_viewed_at 倒序。
     *
     * @param categoryId 可空；非空时限定在该分类内
     */
    List<RecentViewResp> selectRecentViews(@Param("userId") Long userId,
                                           @Param("categoryId") Long categoryId,
                                           @Param("limit") int limit);
}
