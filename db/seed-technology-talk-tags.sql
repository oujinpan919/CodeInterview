-- ============================================================
-- 码上记 CodeInterview · technology-talk 导入后补标签
-- ------------------------------------------------------------
-- seed-technology-talk.sql 只写 question 表，导入的 266 题没有标签，
-- 会导致「按标签筛选」筛不到它们。这里：
--   1. 按题目所属分类给一个「主标签」（已有的 30 个标签内复用）；
--   2. 为消息队列 / 容器 / Nginx 这些原标签体系没覆盖的方向补几个新标签；
--   3. 关键词命中再加附加标签（如 Redis 题里提到持久化 → 持久化）。
--
-- 幂等：全部带 NOT EXISTS 判断，重复执行为 no-op。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1. 新增标签（原 30 个标签未覆盖的方向）
-- ------------------------------------------------------------
INSERT INTO `tag` (`name`) VALUES
  ('消息队列'),
  ('Kafka'),
  ('Docker'),
  ('Kubernetes'),
  ('Nginx'),
  ('系统设计'),
  ('分布式'),
  ('Java 基础'),
  ('面试技巧')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 2. 按分类给主标签（只处理本次导入且尚无标签的题）
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_new_tag`;
CREATE TEMPORARY TABLE `tmp_new_tag` (`qid` BIGINT UNSIGNED, `tid` BIGINT UNSIGNED, PRIMARY KEY (`qid`, `tid`));

-- 分类 → 主标签
-- Java 基础：原标签体系没有对应标签，新建「Java 基础」承载
INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = 'Java 基础'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 100;

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 1 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 101;   -- 集合

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 2 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 102;   -- 并发

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 3 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 103;   -- JVM

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 11 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` IN (160, 163);  -- 数据结构

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 4 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 170;   -- Spring

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 26 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 171;   -- Spring Boot

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 21 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 160;

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 29 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 180;   -- 设计模式

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 15 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 140;   -- 进程与线程

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 16 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 150;   -- TCP/IP

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 9 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 120;   -- 缓存

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = '消息队列'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 206;

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = 'Nginx'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 224;

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = 'Docker'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 230
  AND (q.`title` LIKE '%Docker%' OR q.`answer_md` LIKE '%Docker%');

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = 'Kubernetes'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 230
  AND (q.`title` LIKE '%Kubernetes%' OR q.`answer_md` LIKE '%Kubernetes%'
       OR q.`title` LIKE '%Pod%' OR q.`answer_md` LIKE '%Pod%');

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = '系统设计'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 208;

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = '分布式'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` IN (205, 206, 208);

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = 'Kafka'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 206
  AND (q.`title` LIKE '%Kafka%' OR q.`answer_md` LIKE '%Kafka%');

-- ------------------------------------------------------------
-- 3. 关键词附加标签（让 Redis/MySQL 题能按索引、持久化等筛出来）
-- ------------------------------------------------------------
INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 5 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 110
  AND (q.`title` LIKE '%索引%' OR q.`answer_md` LIKE '%索引%');

-- MySQL 里既不提索引也不提事务/优化的两条，挂「SQL优化」兜底
-- 注意：MySQL 不允许在同一条 INSERT 里既读又写同一张临时表（ERROR 1137），
-- 所以这里改为「排除已命中索引/事务」的写法，而不是 NOT EXISTS 查自身。
INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 8 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 110
  AND q.`title` NOT LIKE '%索引%' AND q.`answer_md` NOT LIKE '%索引%'
  AND q.`title` NOT LIKE '%事务%' AND q.`answer_md` NOT LIKE '%事务%'
  AND q.`title` NOT LIKE '%锁%'   AND q.`answer_md` NOT LIKE '%锁%';

-- HR 类（简历 / 自我介绍）挂「面试技巧」标签
INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, t.`id` FROM `question` q JOIN `tag` t ON t.`name` = '面试技巧'
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 225;

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 6 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 110
  AND (q.`title` LIKE '%事务%' OR q.`answer_md` LIKE '%事务%');

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 8 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 110
  AND (q.`title` LIKE '%优化%' OR q.`answer_md` LIKE '%优化%');

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 7 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`title` LIKE '%锁%';

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 10 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 120
  AND (q.`title` LIKE '%持久化%' OR q.`answer_md` LIKE '%持久化%' OR q.`title` LIKE '%AOF%' OR q.`title` LIKE '%RDB%');

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 27 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 103
  AND (q.`title` LIKE '%GC%' OR q.`title` LIKE '%垃圾%' OR q.`answer_md` LIKE '%垃圾回收%');

INSERT IGNORE INTO `tmp_new_tag` (`qid`, `tid`)
SELECT q.`id`, 28 FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`category_id` = 103
  AND (q.`title` LIKE '%类加载%' OR q.`answer_md` LIKE '%类加载%');

-- ------------------------------------------------------------
-- 4. 落库
-- ------------------------------------------------------------
INSERT IGNORE INTO `question_tag` (`question_id`, `tag_id`)
SELECT `qid`, `tid` FROM `tmp_new_tag`;

DROP TEMPORARY TABLE IF EXISTS `tmp_new_tag`;

-- ------------------------------------------------------------
-- 5. 校验
-- ------------------------------------------------------------
SELECT '导入题仍无标签（应为 0）' AS item, COUNT(*) AS `value`
FROM `question` q
WHERE q.`source` LIKE 'technology-talk%' AND q.`deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `question_tag` qt WHERE qt.`question_id` = q.`id`)
UNION ALL
SELECT '标签总数', COUNT(*) FROM `tag`
UNION ALL
SELECT '发布态题目总数', COUNT(*) FROM `question` WHERE `deleted` = 0 AND `status` = 1;
