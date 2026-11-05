package com.codeinterview.common.util;

/**
 * 分页入参归一化（02 §5.2）：page 从 1 开始，pageSize 默认 10、服务端强制 1–50。
 * 全项目只此一处口径，避免各 Service/Controller 各写一份（review 修复）。
 */
public final class PageUtil {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private PageUtil() {
    }

    public static int normalizePage(Integer rawPage) {
        return rawPage == null || rawPage < 1 ? DEFAULT_PAGE : rawPage;
    }

    public static int normalizePageSize(Integer rawPageSize) {
        if (rawPageSize == null || rawPageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(rawPageSize, MAX_PAGE_SIZE);
    }

    public static long offset(int page, int pageSize) {
        return (long) (page - 1) * pageSize;
    }
}
