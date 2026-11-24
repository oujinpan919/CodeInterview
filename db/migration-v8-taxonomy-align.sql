-- ============================================================
-- 码上记 CodeInterview · v4.6 分类骨架对齐参考站
-- ------------------------------------------------------------
-- 背景：v4.5 把 9 个宽分类拆成 26 个细分方向，但拆得过细（如 MySQL 拆成
--       索引/事务与锁/性能优化三段），且只建了「有题可归」的分类。
-- 本次按用户要求重做为「先搭好分类骨架，题库后续再灌」：
--   1. 完全对齐参考站截图的粒度：计算机网络 1 个、Java 虚拟机 1 个
--      （GC/调优并入）、MySQL 1 个、算法和数据结构 + 数据结构 2 个。
--   2. 补齐截图里当前没有题目的方向（AI/Go/C++/C#/PHP/Python/MyBatis/
--      Netty/Git/Nginx/RabbitMQ/消息队列/后端设计/React/运维/HR 等），
--      先建空分类占位，等题库灌入。
--   3. 不建「Java 热门面试题 200 道 / 前端热门面试题 200 道」这类聚合合集：
--      当前模型一道题只属于一个分类，合集与细分分类互斥。
--   4. 不建 6 个真实面经分类（美团/腾讯/字节/京东/阿里/中小公司）。
--
-- 结果：46 个启用分类 = 截图 54 − 6 面经 − 2 热门合集。
-- 复用 v4.5 已有的分类 id（改名/合并），不产生孤儿分类。
--
-- 幂等：合并语句带源分类条件，跑完后源分类为空，重复执行为 no-op。
-- 回滚：见文件末尾。
-- ============================================================
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1. 补齐截图里有、但还没有的分类（空占位，等题库）
-- ------------------------------------------------------------
INSERT INTO `category` (`id`, `name`, `description`, `sort`, `status`) VALUES
  (200, '最全 AI 应用开发面试题',  '大模型应用开发、RAG、工具调用与工程化',        0, 1),
  (201, 'AI Agent 智能体面试题',   '智能体架构、规划、记忆与多智能体协作',          0, 1),
  (202, 'AI 超级智能体项目面试题', '智能体项目实战：架构设计与落地细节',            0, 1),
  (203, 'Claude Code 面试题',      'AI 编码工具的原理、用法与工程实践',            0, 1),
  (204, 'OpenClaw 面试题',         '开源智能体框架的能力边界与实践',                0, 1),
  (205, 'SpringCloud 面试题',      '服务注册、配置中心、网关与熔断限流',            0, 1),
  (206, '消息队列面试题',          '选型、可靠投递、顺序与幂等、积压处理',          0, 1),
  (207, 'RabbitMQ 面试题',         '交换机模型、确认机制、死信与延迟队列',          0, 1),
  (208, '后端系统设计面试题',      '高并发、高可用、扩展性与容量估算',              0, 1),
  (209, '后端场景面试题',          '真实业务场景下的方案权衡与排障',                0, 1),
  (210, 'MyBatis 面试题',          '映射原理、缓存、动态 SQL 与插件',              0, 1),
  (211, 'Netty 面试题',            'Reactor 模型、ByteBuf、编解码与粘包',           0, 1),
  (212, 'Go 基础面试题',           '语法、并发模型、内存与工程实践',                0, 1),
  (213, 'Go 面试题',               '进阶：调度、GC、性能优化与生态',                0, 1),
  (214, 'C++ 基础面试题',          '语法、STL、内存管理与面向对象',                 0, 1),
  (215, 'C++ 面试题',              '进阶：模板、移动语义、并发与性能',              0, 1),
  (216, 'C# 面试题',               '.NET 运行时、语法特性与常用框架',               0, 1),
  (217, 'PHP 面试题',              '语法、生命周期、框架与性能优化',                0, 1),
  (218, 'Python 面试题',           '语法、GIL、异步与常用库',                       0, 1),
  (219, 'MongoDB 面试题',          '文档模型、索引、分片与副本集',                  0, 1),
  (220, 'React 基础面试题',        'Hooks、虚拟 DOM、状态管理与渲染优化',           0, 1),
  (221, '前端手写代码面试题',      '手写防抖节流、Promise、深拷贝与算法实现',       0, 1),
  (222, '前端代码分析面试题',      '给定代码判断输出、事件循环与执行顺序',          0, 1),
  (223, 'Git 基础面试题',          '分支模型、rebase/merge 与协作流程',            0, 1),
  (224, 'Nginx 面试题',            '反向代理、负载均衡、限流与性能调优',            0, 1),
  (225, 'HR 面试题',               '自我介绍、职业规划、离职原因与软素质',          0, 1),
  (228, '系统运维面试题',          'Linux 运维、监控告警与故障处理',                0, 1),
  (229, 'IT 运维工程师面试题',     'IT 基础设施、网络与服务管理',                   0, 1),
  (230, 'DevOps 运维面试题',       'CI/CD、容器化、自动化与可观测性',              0, 1) ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `status`      = VALUES(`status`);

-- ------------------------------------------------------------
-- 2. 复用 v4.5 的分类：改名 / 合并，回到截图粒度
-- ------------------------------------------------------------

-- 2.1 MySQL：索引 + 事务与锁 + 性能优化 → 单一「MySQL 面试题」
UPDATE `question` SET `category_id` = 110 WHERE `category_id` IN (111, 112);

-- 2.2 Redis：数据结构 + 缓存 + 持久化 → 单一「Redis 面试题」
UPDATE `question` SET `category_id` = 120 WHERE `category_id` IN (121, 122);

-- 2.3 Java 虚拟机：并入垃圾回收 + JVM 调优
UPDATE `question` SET `category_id` = 103 WHERE `category_id` IN (104, 105);

-- 2.4 计算机网络：TCP/IP + HTTP + 进阶 → 单一「计算机网络面试题」
UPDATE `question` SET `category_id` = 150 WHERE `category_id` IN (151, 152);

-- 2.5 数据结构：数组与链表 + 树与堆 + 图论 → 「数据结构面试题」
--     排序 / 动态规划 / 复杂度留在 163「算法和数据结构面试题」
UPDATE `question` SET `category_id` = 160 WHERE `category_id` IN (161, 162);

-- 2.6 前端：浏览器类题目并入 JavaScript（截图无独立「浏览器」分类）
UPDATE `question` SET `category_id` = 130 WHERE `category_id` = 131;

-- 2.7 更名 + 重排 + 更新说明（仍是启用的那批）
UPDATE `category` SET `name` = 'MySQL 面试题',           `description` = '索引、事务与锁、MVCC、优化与架构',       `sort` = 10 WHERE `id` = 110;
UPDATE `category` SET `name` = 'Redis 面试题',           `description` = '数据结构、缓存设计、持久化与高可用',     `sort` = 11 WHERE `id` = 120;
UPDATE `category` SET `name` = 'Java 虚拟机面试题',      `description` = '内存结构、类加载、GC、调优与线上排查',   `sort` = 4  WHERE `id` = 103;
UPDATE `category` SET `name` = '计算机网络面试题',        `description` = '网络模型、TCP/IP、HTTP、DNS 与性能',     `sort` = 17 WHERE `id` = 150;
UPDATE `category` SET `name` = '数据结构面试题',          `description` = '数组链表、栈队列、树与堆、图、哈希',      `sort` = 29 WHERE `id` = 160;
UPDATE `category` SET `name` = '算法和数据结构面试题',    `description` = '排序、查找、动态规划与复杂度分析',        `sort` = 28 WHERE `id` = 163;
UPDATE `category` SET `name` = 'JavaScript 面试题',      `description` = '原型链、闭包、事件循环、浏览器与异步',    `sort` = 21 WHERE `id` = 130;
UPDATE `category` SET `name` = 'Vue 基础面试题',          `description` = '响应式原理、生命周期、组件通信与虚拟 DOM', `sort` = 22 WHERE `id` = 132;
UPDATE `category` SET `sort` = 1  WHERE `id` = 100;  -- Java 基础
UPDATE `category` SET `sort` = 2  WHERE `id` = 101;  -- Java 集合
UPDATE `category` SET `sort` = 3  WHERE `id` = 102;  -- Java 并发
UPDATE `category` SET `sort` = 12 WHERE `id` = 170;  -- Spring
UPDATE `category` SET `sort` = 13 WHERE `id` = 171;  -- SpringBoot
UPDATE `category` SET `sort` = 18 WHERE `id` = 140;  -- 操作系统
UPDATE `category` SET `sort` = 23 WHERE `id` = 180;  -- 设计模式
UPDATE `category` SET `sort` = 30 WHERE `id` = 200;  -- AI 应用开发
UPDATE `category` SET `sort` = 31 WHERE `id` = 201;  -- AI Agent
UPDATE `category` SET `sort` = 32 WHERE `id` = 202;  -- AI 超级智能体项目
UPDATE `category` SET `sort` = 33 WHERE `id` = 203;  -- Claude Code
UPDATE `category` SET `sort` = 34 WHERE `id` = 204;  -- OpenClaw
UPDATE `category` SET `sort` = 14 WHERE `id` = 205;  -- SpringCloud
UPDATE `category` SET `sort` = 15 WHERE `id` = 206;  -- 消息队列
UPDATE `category` SET `sort` = 16 WHERE `id` = 207;  -- RabbitMQ
UPDATE `category` SET `sort` = 19 WHERE `id` = 208;  -- 后端系统设计
UPDATE `category` SET `sort` = 20 WHERE `id` = 209;  -- 后端场景
UPDATE `category` SET `sort` = 24 WHERE `id` = 210;  -- MyBatis
UPDATE `category` SET `sort` = 25 WHERE `id` = 211;  -- Netty
UPDATE `category` SET `sort` = 35 WHERE `id` = 212;  -- Go 基础
UPDATE `category` SET `sort` = 36 WHERE `id` = 213;  -- Go
UPDATE `category` SET `sort` = 37 WHERE `id` = 214;  -- C++ 基础
UPDATE `category` SET `sort` = 38 WHERE `id` = 215;  -- C++
UPDATE `category` SET `sort` = 39 WHERE `id` = 216;  -- C#
UPDATE `category` SET `sort` = 40 WHERE `id` = 217;  -- PHP
UPDATE `category` SET `sort` = 41 WHERE `id` = 218;  -- Python
UPDATE `category` SET `sort` = 26 WHERE `id` = 219;  -- MongoDB
UPDATE `category` SET `sort` = 42 WHERE `id` = 220;  -- React 基础
UPDATE `category` SET `sort` = 43 WHERE `id` = 221;  -- 前端手写代码
UPDATE `category` SET `sort` = 44 WHERE `id` = 222;  -- 前端代码分析
UPDATE `category` SET `sort` = 27 WHERE `id` = 223;  -- Git
UPDATE `category` SET `sort` = 45 WHERE `id` = 224;  -- Nginx
UPDATE `category` SET `sort` = 46 WHERE `id` = 225;  -- HR
UPDATE `category` SET `sort` = 49 WHERE `id` = 228;  -- 系统运维
UPDATE `category` SET `sort` = 50 WHERE `id` = 229;  -- IT 运维
UPDATE `category` SET `sort` = 51 WHERE `id` = 230;  -- DevOps

-- ------------------------------------------------------------
-- 3. 停用被合并掉的细分分类（保留数据，可回滚）
-- ------------------------------------------------------------
UPDATE `category` SET `status` = 0
WHERE `id` IN (104, 105, 111, 112, 121, 122, 131, 151, 152, 161, 162);

-- ------------------------------------------------------------
-- 4. 校验
-- ------------------------------------------------------------
SELECT c.`id`, c.`name`, c.`sort`, COUNT(q.`id`) AS `qn`
FROM `category` c
LEFT JOIN `question` q ON q.`category_id` = c.`id` AND q.`deleted` = 0 AND q.`status` = 1
WHERE c.`status` = 1
GROUP BY c.`id`, c.`name`, c.`sort`
ORDER BY c.`sort`, c.`id`;

SELECT '启用分类数（应为 46）' AS item, COUNT(*) AS `value` FROM `category` WHERE `status` = 1
UNION ALL SELECT '有题分类数', COUNT(DISTINCT category_id) FROM `question`
  WHERE deleted = 0 AND status = 1
UNION ALL SELECT '已归位题目数（应为 718）', COUNT(*) FROM `question` q
  JOIN `category` c ON c.`id` = q.`category_id` AND c.`status` = 1
  WHERE q.`deleted` = 0 AND q.`status` = 1
UNION ALL SELECT '停在停用分类的题（应为 0）', COUNT(*) FROM `question` q
  JOIN `category` c ON c.`id` = q.`category_id` AND c.`status` = 0
  WHERE q.`deleted` = 0 AND q.`status` = 1;

-- ------------------------------------------------------------
-- 回滚：
--   UPDATE question SET category_id=111 WHERE category_id=110 AND EXISTS(索引之外的?)
--   —— 合并类回滚无法精确还原到原子分类，请用 v4.5 的 _bak_qcat_v7 或全量备份恢复：
--   UPDATE question q JOIN _bak_qcat_v7 b ON b.id=q.id SET q.category_id=b.category_id;
--   UPDATE category SET status=1 WHERE id BETWEEN 1 AND 9 OR id BETWEEN 100 AND 180;
-- ------------------------------------------------------------
