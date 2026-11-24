-- ============================================================
-- 码上记 CodeInterview · v4.5 题库分类细化
-- ------------------------------------------------------------
-- 把原来 9 个宽分类拆成 26 个细分方向，每道题归入唯一新分类。
--
-- 【为什么 Java 家族用 id 精确映射，而不是按标签自动分】
--   实测发现题库标签存在系统性偏差：`record` / `sealed` / `switch 表达式` /
--   `接口默认方法` / `StringBuilder` 这类**语言特性**题被误标为 `JVM`，
--   `Optional` 被误标为 `集合`。若按标签拆分，"Java 基础"会被抽成 0 题，
--   大量基础题被错误塞进虚拟机。因此 Java 家族的 81 道题按人工逐条归类，
--   其余分类的标签质量正常，继续用标签规则。
--
-- 【规则】
--   1. 只建有题可归的分类。库里没有 AI/Go/C++/Python/MyBatis/Netty/Git/
--      Nginx/RabbitMQ/MongoDB/运维 等方向的题目，建了会显示 0 题，故留空。
--   2. 真实面经类目（美团/腾讯/字节/京东/阿里/中小公司面经）按要求不建。
--   3. 不足 8 题的零散方向并入上级：类加载机制→虚拟机、网络模型/DNS/
--      拥塞控制/CDN→网络进阶、MySQL Buffer Pool→性能优化、
--      排序/动态规划→数据结构与算法基础。
--   4. 旧分类只置 status=0，不删除，便于回滚。
--
-- 【幂等】分类用固定 id + ON DUPLICATE KEY UPDATE；题目搬迁全部带旧分类条件，
--   第一次跑完后旧分类已空，重复执行是 no-op。
--
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v7-category-split.sql"
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1. 新增 26 个细分分类
-- ------------------------------------------------------------
INSERT INTO `category` (`id`, `name`, `description`, `sort`, `status`) VALUES
  (100, 'Java 基础面试题',          '语法、面向对象、异常、泛型与常用类库',        1,  1),
  (101, 'Java 集合面试题',          'List / Map / Set 实现原理、扩容与线程安全',    2,  1),
  (102, 'Java 并发面试题',          '线程池、锁、AQS、JMM 与并发工具',              3,  1),
  (103, 'Java 虚拟机面试题',        '内存结构、类加载、执行引擎与运行时数据区',     4,  1),
  (104, '垃圾回收面试题',           'GC 算法、分代收集、收集器与核心指标',          5,  1),
  (105, 'JVM 调优面试题',           '参数配置、内存泄漏定位与线上问题排查',         6,  1),
  (110, 'MySQL 索引面试题',         'B+ 树、索引选择、最左前缀与覆盖索引',          7,  1),
  (111, 'MySQL 事务与锁面试题',     'ACID、隔离级别、MVCC 与锁机制',                8,  1),
  (112, 'MySQL 性能优化面试题',     '执行计划、慢查询、Buffer Pool 与架构优化',     9,  1),
  (120, 'Redis 数据结构面试题',     '五大数据类型、底层编码与跳表',                10, 1),
  (121, 'Redis 缓存面试题',         '穿透 / 击穿 / 雪崩、一致性、淘汰策略',         11, 1),
  (122, 'Redis 持久化面试题',       'RDB / AOF、混合持久化与数据恢复',             12, 1),
  (130, 'JavaScript 面试题',        '原型链、闭包、事件循环与异步编程',            13, 1),
  (131, '浏览器面试题',             '渲染流程、缓存、跨域与性能优化',               14, 1),
  (132, 'Vue 面试题',               '响应式原理、生命周期、组件通信与虚拟 DOM',     15, 1),
  (140, '操作系统面试题',           '进程线程、内存管理、IO 模型与调度',           16, 1),
  (150, 'TCP/IP 面试题',            '三次握手、四次挥手、可靠传输与连接管理',       17, 1),
  (151, 'HTTP/HTTPS 面试题',        '报文结构、缓存、HTTP/2 与 TLS',               18, 1),
  (152, '计算机网络进阶面试题',     '网络模型、DNS、CDN、拥塞控制与排障',          19, 1),
  (160, '数组与链表面试题',         '双指针、链表操作与数组技巧',                  20, 1),
  (161, '树与堆面试题',             '二叉树遍历、BST、堆与优先队列',               21, 1),
  (162, '图论面试题',               '遍历、最短路、拓扑排序与并查集',              22, 1),
  (163, '数据结构与算法基础面试题', '排序、动态规划与复杂度分析',                  23, 1),
  (170, 'Spring 面试题',            'IoC / AOP、事务、循环依赖与扩展点',           24, 1),
  (171, 'SpringBoot 面试题',        '自动装配、启动流程与配置管理',                25, 1),
  (180, '设计模式面试题',           '创建型 / 结构型 / 行为型与工程应用',          26, 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `sort`        = VALUES(`sort`),
  `status`      = VALUES(`status`);

-- ------------------------------------------------------------
-- 2. Java 家族（旧 Java 分类，81 行，含 1 道已下架题）人工归类
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_java_map`;
CREATE TEMPORARY TABLE `tmp_java_map` (`qid` BIGINT UNSIGNED NOT NULL PRIMARY KEY, `cid` BIGINT UNSIGNED NOT NULL);

INSERT INTO `tmp_java_map` (`qid`, `cid`) VALUES
  -- Java 基础：语言语法、面向对象、异常、泛型、常用类、JDK 新特性（17）
  (101,100),(208,100),(209,100),(210,100),(211,100),(216,100),(217,100),(220,100),
  (448,100),(449,100),(450,100),(451,100),(452,100),(453,100),(454,100),(455,100),(456,100),
  -- Java 集合：Map/List/Set/Queue 实现、Stream、扩容与迭代器（21）
  (1,101),(102,101),(201,101),(202,101),(218,101),(219,101),
  (401,101),(402,101),(403,101),(404,101),(405,101),(406,101),(407,101),(408,101),
  (409,101),(410,101),(411,101),(412,101),(413,101),(414,101),(415,101),
  -- Java 并发：线程、锁、AQS、JMM、并发工具与线程池（30）
  (103,102),(203,102),(204,102),(205,102),(206,102),(207,102),(212,102),(213,102),(214,102),
  (416,102),(417,102),(418,102),(419,102),(420,102),(421,102),(422,102),(423,102),(424,102),
  (425,102),(426,102),(427,102),(428,102),(429,102),(430,102),(431,102),(432,102),(433,102),
  (434,102),(435,102),(436,102),
  -- Java 虚拟机：内存结构、类加载、JIT、对象创建、引用类型（7）
  (104,103),(215,103),(441,103),(442,103),(444,103),(445,103),(447,103),
  -- 垃圾回收：GC 算法与收集器（3）
  (437,104),(438,104),(439,104),
  -- JVM 调优：泄漏排查、OOM、CPU 飙高（3）
  (440,105),(443,105),(446,105);

UPDATE `question` q
JOIN `tmp_java_map` m ON m.`qid` = q.`id`
SET q.`category_id` = m.`cid`
WHERE q.`category_id` = 1;

-- 兜底：万一有题未纳入映射表，落到 Java 基础
UPDATE `question` SET `category_id` = 100 WHERE `category_id` = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_java_map`;

-- ------------------------------------------------------------
-- 3. 旧 Spring/JVM 分类（按标签拆分，标签质量正常）
-- ------------------------------------------------------------
UPDATE `question` q SET q.`category_id` = 104
WHERE q.`category_id` = 8
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = 'GC');

UPDATE `question` q SET q.`category_id` = 105
WHERE q.`category_id` = 8
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = 'JVM调优');

UPDATE `question` q SET q.`category_id` = 103
WHERE q.`category_id` = 8
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` IN ('JVM', '类加载机制'));

UPDATE `question` q SET q.`category_id` = 171
WHERE q.`category_id` = 8
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = 'Spring Boot');

UPDATE `question` SET `category_id` = 170 WHERE `category_id` = 8;

-- ------------------------------------------------------------
-- 4. MySQL：索引 / 事务与锁 / 性能优化（其余归性能，含 Buffer Pool）
-- ------------------------------------------------------------
UPDATE `question` q SET q.`category_id` = 110
WHERE q.`category_id` = 2
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '索引');

UPDATE `question` q SET q.`category_id` = 111
WHERE q.`category_id` = 2
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` IN ('事务', '锁'));

UPDATE `question` SET `category_id` = 112 WHERE `category_id` = 2;

-- ------------------------------------------------------------
-- 5. Redis：数据结构 / 持久化 / 缓存
-- ------------------------------------------------------------
UPDATE `question` q SET q.`category_id` = 120
WHERE q.`category_id` = 3
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '数据结构');

UPDATE `question` q SET q.`category_id` = 122
WHERE q.`category_id` = 3
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '持久化');

UPDATE `question` SET `category_id` = 121 WHERE `category_id` = 3;

-- ------------------------------------------------------------
-- 6. 前端：Vue / 浏览器 / JavaScript
-- ------------------------------------------------------------
UPDATE `question` q SET q.`category_id` = 132
WHERE q.`category_id` = 4
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = 'Vue');

UPDATE `question` q SET q.`category_id` = 131
WHERE q.`category_id` = 4
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '浏览器');

UPDATE `question` SET `category_id` = 130 WHERE `category_id` = 4;

-- ------------------------------------------------------------
-- 7. 操作系统
-- ------------------------------------------------------------
UPDATE `question` SET `category_id` = 140 WHERE `category_id` = 5;

-- ------------------------------------------------------------
-- 8. 网络：TCP/IP / HTTP / 进阶（模型 + DNS + 拥塞控制 + CDN）
-- ------------------------------------------------------------
UPDATE `question` q SET q.`category_id` = 150
WHERE q.`category_id` = 6
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = 'TCP/IP');

UPDATE `question` q SET q.`category_id` = 151
WHERE q.`category_id` = 6
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = 'HTTP/HTTPS');

UPDATE `question` SET `category_id` = 152 WHERE `category_id` = 6;

-- ------------------------------------------------------------
-- 9. 算法：数组与链表 / 树与堆 / 图论 / 基础（排序+DP+其余）
-- ------------------------------------------------------------
UPDATE `question` q SET q.`category_id` = 160
WHERE q.`category_id` = 7
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '数组与链表');

UPDATE `question` q SET q.`category_id` = 161
WHERE q.`category_id` = 7
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '树');

UPDATE `question` q SET q.`category_id` = 162
WHERE q.`category_id` = 7
  AND EXISTS (SELECT 1 FROM `question_tag` qt JOIN `tag` t ON t.`id` = qt.`tag_id`
              WHERE qt.`question_id` = q.`id` AND t.`name` = '图');

UPDATE `question` SET `category_id` = 163 WHERE `category_id` = 7;

-- ------------------------------------------------------------
-- 10. 设计模式
-- ------------------------------------------------------------
UPDATE `question` SET `category_id` = 180 WHERE `category_id` = 9;

-- ------------------------------------------------------------
-- 11. 停用旧的 9 个宽分类（不删除，便于回滚）
-- ------------------------------------------------------------
UPDATE `category` SET `status` = 0 WHERE `id` BETWEEN 1 AND 9;

-- ------------------------------------------------------------
-- 12. 校验
-- ------------------------------------------------------------
SELECT c.`id`, c.`name`, COUNT(q.`id`) AS `qn`
FROM `category` c
LEFT JOIN `question` q ON q.`category_id` = c.`id` AND q.`deleted` = 0 AND q.`status` = 1
WHERE c.`status` = 1
GROUP BY c.`id`, c.`name`
ORDER BY c.`sort`, c.`id`;

SELECT '旧分类残留（应为 0）' AS item, COUNT(*) AS `value`
FROM `question` WHERE `category_id` BETWEEN 1 AND 9 AND `deleted` = 0
UNION ALL SELECT '启用分类数', COUNT(*) FROM `category` WHERE `status` = 1
UNION ALL SELECT '已归位题目数（应为 718）', COUNT(*) FROM `question` q
  JOIN `category` c ON c.`id` = q.`category_id` AND c.`status` = 1
  WHERE q.`deleted` = 0 AND q.`status` = 1;

-- ------------------------------------------------------------
-- 回滚（需要时手工执行）：
--   UPDATE question q JOIN _bak_qcat_v7 b ON b.id=q.id SET q.category_id=b.category_id;
--   UPDATE category SET status=1 WHERE id BETWEEN 1 AND 9;
--   UPDATE category SET status=0 WHERE id >= 100;
-- ------------------------------------------------------------
