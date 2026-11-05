package com.codeinterview.admin.imports;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 批量导入汇总（04 A3.9 #34 / 03 §7.3）：created/skipped/failed + 逐条错误（index 从 0 开始）。
 */
@Data
public class ImportResult {

    private int created;
    private int skipped;
    private int failed;
    private final List<ImportError> errors = new ArrayList<>();

    public void markCreated() {
        created++;
    }

    public void markSkipped() {
        skipped++;
    }

    public void addError(int index, String reason) {
        failed++;
        errors.add(new ImportError(index, reason));
    }

    @Data
    @AllArgsConstructor
    public static class ImportError {
        private int index;
        private String reason;
    }
}
