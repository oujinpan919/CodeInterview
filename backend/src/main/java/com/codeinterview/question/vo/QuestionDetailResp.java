package com.codeinterview.question.vo;

import lombok.Data;

/**
 * 详情接口完整返回（04 A3.3 #9）：未登录 myFavorite=false、myAssessment=null。
 */
@Data
public class QuestionDetailResp {

    private QuestionDetail question;
    private Boolean myFavorite;
    private Integer myAssessment;
    private NeighborResp neighbors;
}
