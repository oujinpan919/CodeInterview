package com.codeinterview.question;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int increaseViewCount(@Param("id") Long id);
}
