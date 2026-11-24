-- ============================================================
-- 码上记 CodeInterview · v4.12 增量迁移（用户头像）
-- 适用：已运行的现有库；全新库在 init.sql 之后执行同样幂等。
-- 前置：bash scripts/backup.sh
-- 约束：只加列；不删行、不改既有数据。
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v9-user-avatar.sql"
-- 幂等：用 information_schema 判断列是否已存在，可重复执行。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- user.avatar：头像 URL（阿里云 OSS）。
-- 为什么不用单独的头像表：头像与用户是 1:1，且总是一起读（每次 /auth/me 都要），
-- 拆表只会多一次 JOIN；题目配图/笔记配图则不同，它们是 Markdown 内联 URL，不入库。
-- NULL 语义 = 未设置，前端回退为首字母色块，不需要默认图资源。
-- 长度 512：OSS URL（含 bucket、日期分片、UUID、扩展名）约 130 字符，
-- 留出换自定义域名 / CDN 加签名的余量。
-- ------------------------------------------------------------
SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(512) NULL DEFAULT NULL COMMENT ''头像 URL（OSS）；NULL 时前端回退首字母色块'' AFTER `nickname`',
    'SELECT ''user.avatar already exists, skip'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'codeinterview'
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'avatar'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
