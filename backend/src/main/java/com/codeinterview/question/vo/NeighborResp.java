package com.codeinterview.question.vo;

import lombok.Data;

/**
 * 同分类按 id 升序的相邻题；边界返回 null，不循环（03 §8.2）。
 */
@Data
public class NeighborResp {

    private Long prevId;
    private Long nextId;

    public NeighborResp() {
    }

    public NeighborResp(Long prevId, Long nextId) {
        this.prevId = prevId;
        this.nextId = nextId;
    }
}
