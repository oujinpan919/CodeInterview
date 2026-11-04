-- ============================================================
-- 码上记 CodeInterview · 初始化脚本（唯一建表真源）
-- MySQL 8.0.46（本机主路径；所用语法 utf8mb4_0900_ai_ci / DATETIME(3) 默认值 /
--   ON DUPLICATE KEY UPDATE / INSERT IGNORE 均为 8.0 支持项）/ 8.4（容器备选）
-- 执行方式（本机，root 执行一次）：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p < db/init.sql
-- 容器备选：挂载到 /docker-entrypoint-initdb.d 首次启动自动执行
-- ============================================================

CREATE DATABASE IF NOT EXISTS `codeinterview`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `codeinterview`;

-- ------------------------------------------------------------
-- 1. 用户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nickname`      VARCHAR(20)     NOT NULL                COMMENT '昵称=登录名+展示名；2-20 字，可中文；全站唯一（大小写不敏感）',
  `password`      VARCHAR(72)     NOT NULL                COMMENT 'BCrypt 哈希（cost=12，60 字符，留余量）',
  `role`          TINYINT         NOT NULL DEFAULT 0      COMMENT '0 普通用户 / 1 管理员',
  `status`        TINYINT         NOT NULL DEFAULT 1      COMMENT '1 正常 / 0 禁用',
  `last_login_at` DATETIME(3)     NULL                    COMMENT '最近登录时间',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0/1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户（昵称即登录名）';

-- ------------------------------------------------------------
-- 2. 分类（一级知识域，无逻辑删除）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(30)     NOT NULL              COMMENT '分类名，全局唯一',
  `description` VARCHAR(200)    NOT NULL DEFAULT ''   COMMENT '一句话说明',
  `sort`        INT             NOT NULL DEFAULT 0    COMMENT '升序排序，越小越前',
  `status`      TINYINT         NOT NULL DEFAULT 1    COMMENT '1 启用 / 0 停用（停用后前台不展示、新题不可选）',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目分类';

-- ------------------------------------------------------------
-- 3. 标签（细粒度知识点）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tag` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(20)     NOT NULL            COMMENT '标签名，全局唯一',
  `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识点标签';

-- ------------------------------------------------------------
-- 4. 题目
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id`    BIGINT UNSIGNED NOT NULL              COMMENT '所属分类',
  `title`          VARCHAR(120)    NOT NULL              COMMENT '标题，1-120 字',
  `content_md`     MEDIUMTEXT      NOT NULL              COMMENT '题干 Markdown，1-20000 字符',
  `answer_md`      MEDIUMTEXT      NOT NULL              COMMENT '参考答案 Markdown，1-20000 字符，必填',
  `difficulty`     TINYINT         NOT NULL DEFAULT 2    COMMENT '1 简单 / 2 中等 / 3 困难',
  `status`         TINYINT         NOT NULL DEFAULT 0    COMMENT '0 草稿 / 1 发布 / 2 下架',
  `source`         VARCHAR(50)     NOT NULL DEFAULT ''   COMMENT '来源说明，仅后台可见',
  `view_count`     INT UNSIGNED    NOT NULL DEFAULT 0    COMMENT '浏览次数',
  `practice_count` INT UNSIGNED    NOT NULL DEFAULT 0    COMMENT '被自评次数（热度）',
  `published_at`   DATETIME(3)     NULL                  COMMENT '首次发布时间，列表排序用',
  `created_by`     BIGINT UNSIGNED NOT NULL              COMMENT '创建者 user.id',
  `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted`        TINYINT         NOT NULL DEFAULT 0    COMMENT '逻辑删除 0/1',
  PRIMARY KEY (`id`),
  KEY `idx_status_published` (`status`, `published_at`),
  KEY `idx_category_status` (`category_id`, `status`),
  KEY `idx_practice_count` (`practice_count`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目';

-- ------------------------------------------------------------
-- 5. 题目-标签关联
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_tag` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT UNSIGNED NOT NULL,
  `tag_id`      BIGINT UNSIGNED NOT NULL,
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_tag` (`question_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目标签关联';

-- ------------------------------------------------------------
-- 6. 收藏
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_favorite` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `question_id` BIGINT UNSIGNED NOT NULL,
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏（取消即物理删除）';

-- ------------------------------------------------------------
-- 7. 刷题记录（自评流水，只增）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practice_record` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `question_id` BIGINT UNSIGNED NOT NULL,
  `self_assess` TINYINT         NOT NULL              COMMENT '1 掌握 / 2 模糊 / 3 不会',
  `source`      TINYINT         NOT NULL DEFAULT 1    COMMENT '1 详情页 / 2 随机刷题（V1 不用） / 3 错题本复习',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_user_question` (`user_id`, `question_id`, `created_at`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题记录（只增不改不删）';

-- ------------------------------------------------------------
-- 8. 错题本
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wrong_record` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT UNSIGNED NOT NULL,
  `question_id`    BIGINT UNSIGNED NOT NULL,
  `last_assess`    TINYINT         NOT NULL              COMMENT '最近一次自评档位 1/2/3',
  `wrong_count`    INT UNSIGNED    NOT NULL DEFAULT 1    COMMENT '累计答错（自评 2/3）次数',
  `first_wrong_at` DATETIME(3)     NOT NULL              COMMENT '首次进入错题本时间',
  `last_wrong_at`  DATETIME(3)     NOT NULL              COMMENT '最近一次答错时间',
  `resolved`       TINYINT         NOT NULL DEFAULT 0    COMMENT '0 在错题本 / 1 已掌握',
  `mastered_at`    DATETIME(3)     NULL                  COMMENT '标记掌握时间，回到 0 时清空',
  `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_resolved` (`user_id`, `resolved`, `last_wrong_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本';

-- ------------------------------------------------------------
-- 9. AI 账号日额度
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_ai_quota` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `stat_date`  DATE            NOT NULL              COMMENT '统计日（Asia/Shanghai）',
  `used_count` INT UNSIGNED    NOT NULL DEFAULT 0    COMMENT '当日已用条数',
  `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 账号日额度';

-- ------------------------------------------------------------
-- 10. AI 全站日预算
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `site_ai_usage` (
  `stat_date`   DATE         NOT NULL,
  `used_count`  INT UNSIGNED NOT NULL DEFAULT 0    COMMENT '当日全站已用条数',
  `used_tokens` BIGINT       NOT NULL DEFAULT 0    COMMENT '当日累计 tokens（估算或真实）',
  `updated_at`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 全站日预算';

-- ------------------------------------------------------------
-- 11. AI 调用审计日志（不含会话内容）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_log` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`           BIGINT UNSIGNED NOT NULL,
  `session_id`        VARCHAR(64)     NULL            COMMENT '前端 X-Session-Id，可空',
  `question_id`       BIGINT UNSIGNED NULL            COMMENT '带入的题目 id，可空',
  `model`             VARCHAR(64)     NOT NULL        COMMENT '实际使用的模型名',
  `prompt_tokens`      INT            NOT NULL DEFAULT 0,
  `completion_tokens`  INT            NOT NULL DEFAULT 0,
  `total_tokens`       INT            NOT NULL DEFAULT 0,
  `estimated`         TINYINT         NOT NULL DEFAULT 0 COMMENT '1 表示 usage 缺失、按字符估算',
  `first_token_ms`    INT             NULL            COMMENT '首 token 耗时',
  `latency_ms`        INT             NULL            COMMENT '总耗时',
  `status`            TINYINT         NOT NULL        COMMENT '1 正常 / 2 失败 / 3 客户端中断或停止 / 4 流前拒绝',
  `error_code`        INT             NULL            COMMENT '本项目业务码（如 42901/50201），流前拒绝或失败时填',
  `upstream_code`     INT             NULL            COMMENT '上游原始错误码（讯飞：10007/11201 等），用于定位供应商侧问题',
  `created_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 调用审计（不存消息正文）';

-- 分类（5 个）
INSERT INTO `category` (`name`, `description`, `sort`) VALUES
  ('Java',     'Java 基础、集合、并发、JVM', 1),
  ('MySQL',    '索引、事务、锁、优化',        2),
  ('Redis',    '数据结构、持久化、缓存设计',  3),
  ('前端',     'JavaScript、浏览器、框架',    4),
  ('操作系统', '进程线程、内存、IO',          5);


-- 标签（15 个）
INSERT INTO `tag` (`name`) VALUES
  ('集合'),('并发'),('JVM'),('Spring'),
  ('索引'),('事务'),('锁'),('SQL优化'),
  ('缓存'),('持久化'),('数据结构'),
  ('JavaScript'),('浏览器'),('Vue'),
  ('进程与线程');

-- 示例题（3 道，均为草稿，需管理员发布后前台可见）
-- 【created_by 说明】下面的 created_by = 1 是**占位值**，只要求库中 user 表存在 id=1 的行
--   （管理员由启动初始化器创建，通常是第一个用户，见下方「管理员账号」说明）。
--   若你的管理员 id 不是 1，请在导入前把 created_by 改成实际管理员 id；
--   即使不一致也不影响前台展示（该字段只用于审计），但**不要**为它建物理外键。
--   重跑 seed 前请先 TRUNCATE 相关表，否则 category/tag 会撞 uk_name、question_tag 会撞
--   uk_question_tag；question 本身无唯一键，重跑只会产生重复行（不会报错），同样应清空。
INSERT INTO `question`
  (`category_id`, `title`, `content_md`, `answer_md`, `difficulty`, `status`, `source`, `created_by`)
VALUES
  (1, 'HashMap 的扩容机制是怎样的？',
   '请说明：\n\n1. 扩容的触发条件\n2. 扩容过程（rehash）\n3. JDK 8 中红黑树对扩容的影响\n4. 并发扩容会有什么问题',
   '**触发条件**：元素个数超过 `容量 × 负载因子(0.75)` 时扩容。\n\n**过程**：容量翻倍，重新计算每个桶的位置。JDK 8 利用 `hash & oldCap` 判断是否需要迁移，避免重新计算 hash。\n\n**红黑树**：扩容后若树节点 ≤6 会退化为链表。\n\n**并发问题**：JDK 7 头插法在并发扩容时可能形成环形链表导致死循环；JDK 8 改为尾插法不再成环，但仍会丢数据。并发场景应使用 `ConcurrentHashMap`。',
   2, 0, '原创', 1),
  (2, 'MySQL 为什么用 B+ 树做索引？',
   '从磁盘 IO、范围查询、树高三个角度回答。',
   '1. **矮胖**：非叶子节点只存键，一页能放更多键，三到四层可支撑千万级数据，减少磁盘 IO。\n2. **范围查询**：叶子节点用双向链表连接，范围扫描只需顺序遍历。\n3. **稳定**：所有数据都在叶子节点，每次查询路径长度一致。\n\n对比 B 树：B 树非叶子节点也存数据，扇出小、树更高；且范围查询需要中序遍历回溯。',
   2, 0, '原创', 1),
  (3, 'Redis 的持久化方式有哪些？如何选择？',
   '说明 RDB 与 AOF 的原理、优缺点与选型。',
   '**RDB**：周期性生成全量快照。优点是文件紧凑、恢复快；缺点是可能丢最后一次快照之后的数据，fork 时会有内存与延迟抖动。\n\n**AOF**：追加写命令日志，可配置 `everysec`/`always`/`no`。优点是丢数据少；缺点是文件大、恢复慢。\n\n**选型**：允许丢少量数据用 RDB；要求高可靠用 AOF `everysec`；生产常用「RDB + AOF 混合持久化」。',
   2, 0, '原创', 1);

-- 示例题的标签关联
INSERT INTO `question_tag` (`question_id`, `tag_id`) VALUES
  (1, 1), (1, 3), (2, 5), (2, 8), (3, 9), (3, 10);
