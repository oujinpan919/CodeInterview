package com.codeinterview.home.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.codeinterview.question.vo.QuestionBriefResp;

import lombok.Data;

/**
 * 首页聚合（04 A3.2 #7）；continueLearning 未登录时为 null。
 */
@Data
public class HomeResp {

    private List<HomeCategory> categories;
    private List<QuestionBriefResp> latest;
    private List<QuestionBriefResp> hot;
    private List<HomeContinueLearning> continueLearning;

    @Data
    public static class HomeCategory {

        private Long id;
        private String name;
        private String description;
        private Integer questionCount;
    }

    @Data
    public static class HomeContinueLearning {

        private Long id;
        private String title;
        private Integer lastAssess;
        private LocalDateTime lastAssessedAt;
    }
}
