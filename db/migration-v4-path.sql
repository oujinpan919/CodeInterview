-- ============================================================
-- 码上记 CodeInterview · v3.6 批次六 增量迁移（C8 刷题路线）
-- 适用：已运行的现有库；全新库在 init.sql 之后执行同样幂等。
-- 前置：bash scripts/backup.sh
-- 约束：只 CREATE TABLE IF NOT EXISTS + INSERT IGNORE；不动既有表与数据。
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v4-path.sql"
-- 幂等：建表 IF NOT EXISTS；路线用固定 id + uk_name，题目项用 uk_path_question + INSERT IGNORE。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1) 第 14、15 张表：刷题路线 + 路线题目（有序题单）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `learning_path` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(80)     NOT NULL              COMMENT '路线名（唯一）',
  `description` VARCHAR(255)    NOT NULL DEFAULT ''   COMMENT '一句话说明',
  `sort`        INT             NOT NULL DEFAULT 0    COMMENT '排序（小的在前）',
  `status`      TINYINT         NOT NULL DEFAULT 1    COMMENT '1 启用 / 0 停用',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题路线（v3.6 C8）';

CREATE TABLE IF NOT EXISTS `learning_path_item` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `path_id`     BIGINT UNSIGNED NOT NULL              COMMENT 'learning_path.id',
  `question_id` BIGINT UNSIGNED NOT NULL              COMMENT 'question.id',
  `sort`        INT             NOT NULL DEFAULT 0    COMMENT '路线内序号（从 1 开始，按由易到难）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path_question` (`path_id`, `question_id`),
  KEY `idx_path_sort` (`path_id`, `sort`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线题目（有序题单；v3.6 C8）';

-- ------------------------------------------------------------
-- 2) 4 条路线（固定 id；幂等）
-- ------------------------------------------------------------
INSERT IGNORE INTO `learning_path` (`id`, `name`, `description`, `sort`, `status`) VALUES
  (1, 'Java 基础冲刺 30 题', '按由易到难排：语言基础 → 集合 → 并发 → JVM 相关，适合面试前一天过一遍', 1, 1),
  (2, 'MySQL 与 Redis 24 题', '数据库与缓存高频考点：索引、事务、锁、持久化、缓存一致性', 2, 1),
  (3, '算法与数据结构 24 题', '数组/链表 → 树/图 → 排序 → 动态规划，题目按难度递增', 3, 1),
  (4, 'Spring / JVM 24 题', 'IoC/AOP、类加载、GC、JVM 调优等后端必问项', 4, 1);

-- ------------------------------------------------------------
-- 3) 路线题目（按 difficulty ASC, id ASC 取该分类前 N 道发布态题；幂等）
-- ------------------------------------------------------------
INSERT IGNORE INTO `learning_path_item` (`path_id`, `question_id`, `sort`)
SELECT 1, q.id, ROW_NUMBER() OVER (ORDER BY q.difficulty ASC, q.id ASC)
FROM `question` q
WHERE q.category_id = 1 AND q.status = 1 AND q.deleted = 0
ORDER BY q.difficulty ASC, q.id ASC
LIMIT 30;

INSERT IGNORE INTO `learning_path_item` (`path_id`, `question_id`, `sort`)
SELECT 2, q.id, ROW_NUMBER() OVER (ORDER BY q.difficulty ASC, q.id ASC)
FROM `question` q
WHERE q.category_id IN (2, 3) AND q.status = 1 AND q.deleted = 0
ORDER BY q.difficulty ASC, q.id ASC
LIMIT 24;

INSERT IGNORE INTO `learning_path_item` (`path_id`, `question_id`, `sort`)
SELECT 3, q.id, ROW_NUMBER() OVER (ORDER BY q.difficulty ASC, q.id ASC)
FROM `question` q
WHERE q.category_id = 7 AND q.status = 1 AND q.deleted = 0
ORDER BY q.difficulty ASC, q.id ASC
LIMIT 24;

INSERT IGNORE INTO `learning_path_item` (`path_id`, `question_id`, `sort`)
SELECT 4, q.id, ROW_NUMBER() OVER (ORDER BY q.difficulty ASC, q.id ASC)
FROM `question` q
WHERE q.category_id = 8 AND q.status = 1 AND q.deleted = 0
ORDER BY q.difficulty ASC, q.id ASC
LIMIT 24;

-- ------------------------------------------------------------
-- 4) 校验
-- ------------------------------------------------------------
SELECT p.id, p.name, COUNT(i.id) AS item_count
FROM `learning_path` p
LEFT JOIN `learning_path_item` i ON i.path_id = p.id
GROUP BY p.id, p.name
ORDER BY p.sort;
