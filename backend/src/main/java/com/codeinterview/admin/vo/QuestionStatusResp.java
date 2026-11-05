package com.codeinterview.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发布 / 下架响应（04 A3.9 #32/#33）：{ "status": 1|2 }。
 */
@Data
@AllArgsConstructor
public class QuestionStatusResp {

    private Integer status;
}
