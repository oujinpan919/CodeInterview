package com.codeinterview.favorite.vo;

import java.time.LocalDateTime;

import com.codeinterview.question.vo.QuestionBriefResp;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的收藏行（04 A3.3 #13）：QuestionBrief + favoritedAt。
 * status=2 仍返回，由前端置灰（03 §8.3）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FavoriteQuestionResp extends QuestionBriefResp {

    private LocalDateTime favoritedAt;
}
