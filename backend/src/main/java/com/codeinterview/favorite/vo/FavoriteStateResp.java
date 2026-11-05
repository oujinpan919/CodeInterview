package com.codeinterview.favorite.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 收藏 / 取消收藏的响应（04 A3.3 #11 / #12：{ "favorited": true|false }）。
 */
@Data
@AllArgsConstructor
public class FavoriteStateResp {

    private Boolean favorited;
}
