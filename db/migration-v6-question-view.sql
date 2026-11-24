-- ============================================================
-- 码上记 CodeInterview · v4.1 增量迁移（题库列表「已读」标记）
-- 适用：已运行的现有库；全新库在 init.sql 之后执行同样幂等。
-- 前置：bash scripts/backup.sh
-- 约束：只 CREATE TABLE IF NOT EXISTS；不删行、不改既有表。
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v6-question-view.sql"
-- 幂等：CREATE TABLE IF NOT EXISTS 已存在时 no-op，可重复执行。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 第 16 张表：题目浏览记录（每用户每题至多 1 行）
-- 用途：题库列表里把「没打开过」的题目标成蓝色、「打开过」的标成绿色。
-- 口径：用户在题目详情页成功加载一次 = 一次访问；同一题反复访问只更新 last_viewed_at。
-- 注意：与 question.view_count 不是一回事 —— 那个是全站累计计数，不区分用户。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_view` (
  `user_id`         BIGINT UNSIGNED NOT NULL                              COMMENT '浏览者 user.id；只从登录态取，不建物理外键',
  `question_id`     BIGINT UNSIGNED NOT NULL                              COMMENT '题目 question.id',
  `first_viewed_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次打开时间（保留第一次，不覆盖）',
  `last_viewed_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近一次打开时间（每次访问显式更新）',
  PRIMARY KEY (`user_id`, `question_id`),
  KEY `idx_user_last` (`user_id`, `last_viewed_at`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='题目浏览记录（v4.1；题库列表已读标记的数据源）';

-- ------------------------------------------------------------
-- 校验
-- ------------------------------------------------------------
SELECT 'question_view 表存在' AS item, COUNT(*) AS value
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'question_view'
UNION ALL SELECT '当前浏览记录条数', COUNT(*) FROM `question_view`;
