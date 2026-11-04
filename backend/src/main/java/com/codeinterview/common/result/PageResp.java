package com.codeinterview.common.result;

import lombok.Data;

import java.util.List;

@Data
public class PageResp<T> {

    private long page;
    private long size;
    private long total;
    private long pages;
    private List<T> list;

    public PageResp() {
    }

    public PageResp(long page, long size, long total, long pages, List<T> list) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.pages = pages;
        this.list = list;
    }

    public static <T> PageResp<T> of(long page, long size, long total, List<T> list) {
        long normalizedTotal = Math.max(total, 0);
        long pages = (size <= 0) ? 0 : (normalizedTotal + size - 1) / size;
        return new PageResp<>(page, size, normalizedTotal, pages, list == null ? List.of() : list);
    }
}
