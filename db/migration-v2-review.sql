-- ============================================================
-- 码上记 CodeInterview · v3.6 批次一 增量迁移（C1 间隔重复排程 + C5 今日计划 + 修2 覆盖率）
-- 适用：已运行的现有库（含演示题与用户数据）；全新库在 init.sql 之后执行同样幂等。
-- 前置：执行前先全量备份 —— bash scripts/backup.sh
-- 约束：只 ALTER 加列/放宽可空性 + 幂等回填；不 DROP 表、不删行、不覆盖用户已有自评数据。
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v2-review.sql"
-- 幂等：每步先查 information_schema 再决定执行，可重复跑。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1) wrong_record 升级为「复习条目（review item）」
--    - first_wrong_at / last_wrong_at 放宽为 NULL：自评「掌握」但从未答错的题也会建行，
--      这类行没有"答错"语义，塞 now() 会污染错题次数统计（v3.6 变更）。
--    - due_at / interval_days / review_streak：间隔重复排程三列。
-- ------------------------------------------------------------
SET @s = (SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `wrong_record` MODIFY COLUMN `first_wrong_at` DATETIME(3) NULL COMMENT ''首次进入错题本时间；从未答错则为 NULL''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wrong_record'
    AND column_name = 'first_wrong_at' AND is_nullable = 'NO');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `wrong_record` MODIFY COLUMN `last_wrong_at` DATETIME(3) NULL COMMENT ''最近一次答错时间；从未答错则为 NULL''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wrong_record'
    AND column_name = 'last_wrong_at' AND is_nullable = 'NO');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `wrong_record` ADD COLUMN `due_at` DATETIME(3) NULL COMMENT ''下次复习到期时间；NULL=已退出复习队列（真掌握）或未排程''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wrong_record' AND column_name = 'due_at');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `wrong_record` ADD COLUMN `interval_days` INT NOT NULL DEFAULT 0 COMMENT ''当前复习间隔（天）；0=未排程/已退出队列''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wrong_record' AND column_name = 'interval_days');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `wrong_record` ADD COLUMN `review_streak` INT NOT NULL DEFAULT 0 COMMENT ''连续「掌握」次数；达到 4 判真掌握并退出复习队列''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wrong_record' AND column_name = 'review_streak');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2) 复习到期索引（今日计划按 due_at 取数）
-- ------------------------------------------------------------
SET @s = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `wrong_record` ADD KEY `idx_user_due` (`user_id`, `due_at`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'wrong_record' AND index_name = 'idx_user_due');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3) 幂等回填（只填尚未排程的行，不动已有 due_at）
--    3.1 仍在错题本（resolved=0）→ 立即到期，进入今日计划
--    3.2 已掌握（resolved=1）→ 次日回顾一次（掌握待回顾）
--    3.3 只自评过「掌握」、从未进过错题本的题 → 补建行（v3.6：所有自评过的题都进排程）
-- ------------------------------------------------------------
UPDATE `wrong_record`
SET `due_at` = NOW(3), `interval_days` = 1, `review_streak` = 0
WHERE `resolved` = 0 AND `due_at` IS NULL;

UPDATE `wrong_record`
SET `due_at` = DATE_ADD(NOW(3), INTERVAL 1 DAY), `interval_days` = 1, `review_streak` = 1
WHERE `resolved` = 1 AND `due_at` IS NULL AND `review_streak` = 0;

INSERT IGNORE INTO `wrong_record`
  (`user_id`, `question_id`, `last_assess`, `wrong_count`, `first_wrong_at`, `last_wrong_at`,
   `resolved`, `mastered_at`, `due_at`, `interval_days`, `review_streak`, `created_at`, `updated_at`)
SELECT pr.user_id, pr.question_id, 1, 0, NULL, NULL,
       1, MAX(pr.created_at), DATE_ADD(NOW(3), INTERVAL 1 DAY), 1, 1, NOW(3), NOW(3)
FROM `practice_record` pr
WHERE pr.self_assess = 1
  AND NOT EXISTS (
    SELECT 1 FROM `wrong_record` w
    WHERE w.user_id = pr.user_id AND w.question_id = pr.question_id
  )
  AND NOT EXISTS (
    SELECT 1 FROM `practice_record` p2
    WHERE p2.user_id = pr.user_id AND p2.question_id = pr.question_id
      AND p2.self_assess <> 1
  )
GROUP BY pr.user_id, pr.question_id;

-- ------------------------------------------------------------
-- 4) 校验（执行后人工核对输出）
-- ------------------------------------------------------------
SELECT 'wrong_record 总行数' AS item, COUNT(*) AS value FROM `wrong_record`
UNION ALL SELECT '待复习（due_at 非空）', COUNT(*) FROM `wrong_record` WHERE `due_at` IS NOT NULL
UNION ALL SELECT '复习队列外（真掌握）', COUNT(*) FROM `wrong_record` WHERE `due_at` IS NULL
UNION ALL SELECT '发布态题目数（覆盖率分母）', COUNT(*) FROM `question` WHERE `status` = 1 AND `deleted` = 0;
