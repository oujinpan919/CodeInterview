-- ============================================================
-- 码上记 CodeInterview · V1 功能扩展批次 增量迁移脚本
-- 适用：已运行的现有库（含 18 演示题与用户数据）。全新安装也可用（幂等）。
-- 前置：执行前必须先做一次全量备份（见下方备份命令）。
-- 约束：只 CREATE TABLE IF NOT EXISTS + INSERT IGNORE；
--       全程不出现任何针对既有数据的 DROP / TRUNCATE / DELETE / UPDATE。
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v1-expansion.sql"
-- 幂等：CREATE TABLE IF NOT EXISTS 已建表时 no-op；
--       category 靠固定 id 6-9 + uk_name、tag 靠固定 id 16-30 + uk_name，
--       INSERT IGNORE 重复执行不会产生重复行（E-09）。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1) 第 12 张表：练习会话（与 init.sql 追加定义逐字一致）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practice_session` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                COMMENT '练习会话 id',
  `user_id`         BIGINT UNSIGNED NOT NULL                               COMMENT '归属用户 user.id；只从登录态取，不建物理外键',
  `status`          TINYINT         NOT NULL DEFAULT 1                      COMMENT '1 进行中 / 2 已完成 / 3 已放弃 / 4 已完成（部分跳过）',
  `active_flag`     TINYINT         NULL     DEFAULT 1                      COMMENT '进行中=1，结束态=NULL；配合 uk_user_active 保证同一用户至多 1 个进行中（NULL 不参与唯一约束）',
  `question_ids`    JSON            NOT NULL                                COMMENT '会话题序：有序 question.id JSON 数组，如 [201,205,318]；长度即总题数 M',
  `total_count`     INT UNSIGNED    NOT NULL DEFAULT 0                      COMMENT '总题数 M = JSON_LENGTH(question_ids)，冗余避免每次解析 JSON',
  `current_index`   INT UNSIGNED    NOT NULL DEFAULT 0                      COMMENT '当前断点下标（0-based，指向 question_ids）；只单调前进，不回退',
  `completed_count` INT UNSIGNED    NOT NULL DEFAULT 0                      COMMENT '已完成题数 X；按题去重 +1（E-07）',
  `skipped_count`   INT UNSIGNED    NOT NULL DEFAULT 0                      COMMENT '因下架/删除/分类停用而跳过的题数 S；完成态满足 X = M - S（E-04/AC-E10）',
  `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_active` (`user_id`, `active_flag`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='练习会话（进度落库；保留历史不物理删除）';

-- ------------------------------------------------------------
-- 2) 新增 4 个分类（id 6-9；status=1，sort 续接 6-9；A-11）
--    执行前请核对 SELECT MAX(id) FROM category = 5、SELECT MAX(id) FROM tag = 15
-- ------------------------------------------------------------
INSERT IGNORE INTO `category` (`id`, `name`, `description`, `sort`, `status`) VALUES
  (6, '计算机网络',     'TCP/IP、HTTP、DNS、网络模型与拥塞控制', 6, 1),
  (7, '算法与数据结构', '数组链表、树与图、排序、动态规划',       7, 1),
  (8, 'Spring/JVM',     'IoC/AOP、类加载、GC、并发与调优',        8, 1),
  (9, '设计模式',       '创建型、结构型、行为型模式与实践',       9, 1);

-- ------------------------------------------------------------
-- 3) [架构决策] 新增 15 个标签（固定 id 16-30），见 A.4 标签策略
-- ------------------------------------------------------------
INSERT IGNORE INTO `tag` (`id`, `name`) VALUES
  (16, 'TCP/IP'),     (17, 'HTTP/HTTPS'), (18, 'DNS'),        (19, '网络模型'), (20, '拥塞控制'),
  (21, '数组与链表'), (22, '树'),         (23, '图'),         (24, '排序'),     (25, '动态规划'),
  (26, 'Spring Boot'),(27, 'GC'),         (28, '类加载机制'), (29, '设计模式'), (30, 'JVM调优');

-- ============================================================
-- 备份命令（执行本脚本前，站长/主理人先做一次全量备份，对齐 README §7.6）
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysqldump.exe" -uroot -p \
--     --default-character-set=utf8mb4 --single-transaction --routines \
--     codeinterview > backups/codeinterview_before_v1expansion_YYYYMMDD_HHMMSS.sql
-- 或复用既有 scripts/backup.sh（输出 backups/codeinterview_<timestamp>.sql.gz）
-- ============================================================
