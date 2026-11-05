package com.codeinterview.practice.vo;

import lombok.Data;

/**
 * 自评接口返回的错题状态（04 A3.4 #14）：
 * null = 从未进过错题本（自评 1 且历史上没进过本）；有值 = 在错题本(resolved=0) 或已掌握(resolved=1)。
 */
@Data
public class WrongBookState {

    private Integer resolved;

    private Integer wrongCount;
}
