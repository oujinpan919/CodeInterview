package com.codeinterview.practice.dto;

import lombok.Data;

/**
 * 创建练习会话入参（04 A3.4 #13；v3.6 增 mode）。
 *
 * <p>mode：random（默认，按筛选随机抽题）/ today（今日计划：先到期复习、再新题补足）。
 * today 模式下 categoryId/tagId/difficulty 被忽略，size 表示今日计划总题数。
 */
@Data
public class SessionCreateReq {

    private Long categoryId;

    private Long tagId;

    private Integer difficulty;

    private Integer size;

    private String mode;
}
