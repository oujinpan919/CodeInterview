-- ============================================================
-- 码上记 CodeInterview · v3.6 批次三 增量迁移（C6 个人笔记 + C7 笔记导出）
-- 适用：已运行的现有库；全新库在 init.sql 之后执行同样幂等。
-- 前置：bash scripts/backup.sh
-- 约束：只 CREATE TABLE IF NOT EXISTS；不删行、不改既有表。
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v3-note.sql"
-- 幂等：CREATE TABLE IF NOT EXISTS 已存在时 no-op，可重复执行。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 第 13 张表：题目私有笔记（每用户每题至多 1 条；清空即删除，不留空行）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_note` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                  COMMENT '笔记 id',
  `user_id`     BIGINT UNSIGNED NOT NULL                                 COMMENT '归属用户 user.id；只从登录态取，不建物理外键',
  `question_id` BIGINT UNSIGNED NOT NULL                                 COMMENT '题目 question.id',
  `content_md`  MEDIUMTEXT      NOT NULL                                 COMMENT '笔记正文 Markdown，≤20000 字符（服务端校验）',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)    COMMENT '首次写下时间',
  `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后保存时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_updated` (`user_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目私有笔记（v3.6 C6；导出见 C7）';

-- ------------------------------------------------------------
-- 校验
-- ------------------------------------------------------------
SELECT 'question_note 表存在' AS item, COUNT(*) AS value
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'question_note'
UNION ALL SELECT '当前笔记条数', COUNT(*) FROM `question_note`;
