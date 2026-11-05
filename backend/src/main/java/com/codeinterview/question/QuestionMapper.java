package com.codeinterview.question;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionDetail;
import com.codeinterview.question.vo.QuestionTagRow;

@Mapper
public interface QuestionMapper {

    long countQuestionPage(@Param("query") QuestionQueryReq query);

    List<QuestionBriefResp> selectQuestionPage(@Param("query") QuestionQueryReq query,
                                               @Param("offset") long offset,
                                               @Param("size") int size);

    List<QuestionTagRow> selectTagsByQuestionIds(@Param("ids") List<Long> ids);

    QuestionDetail selectPublishedDetail(@Param("id") Long id);

    Long selectPrevId(@Param("categoryId") Long categoryId, @Param("id") Long id);

    Long selectNextId(@Param("categoryId") Long categoryId, @Param("id") Long id);

    Integer selectFavoriteFlag(@Param("userId") Long userId, @Param("questionId") Long questionId);

    Integer selectLatestAssessment(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Update("UPDATE question SET practice_count = practice_count + 1 WHERE id = #{id} AND deleted = 0")
    int increasePracticeCount(@Param("id") Long id);

    /** 发布态存在性判断（自评/收藏前置校验）：只查 COUNT，不拉整行。 */
    @Select("SELECT COUNT(*) FROM question WHERE id = #{id} AND status = 1 AND deleted = 0")
    int countPublished(@Param("id") Long id);

    int increaseViewCount(@Param("id") Long id);
}
