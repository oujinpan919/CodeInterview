-- ============================================================
-- 码上记 CodeInterview · 演示数据（T18）
-- 前置：已执行 db/init.sql（11 张表 + 5 分类 / 15 标签 / 3 道草稿题）
-- 执行：
--   "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 \
--     -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-demo.sql"
-- 说明：
--   1) 在 init.sql 之后只执行一次；固定自增 id + INSERT IGNORE，重复执行不会重复插入；
--   2) 3 道 seed 草稿题（id 1-3）保持 status=0 不发布：AI 单测依赖 q1 的草稿态；
--   3) admin / demo 两个账号密码均为 123456（BCrypt cost=12 固定哈希）；admin 已存在时跳过，不覆盖密码；
--   4) 演示题目 id 101-118；自评/错题/收藏 id 1001+，便于识别与清理；
--   5) 覆盖 5 个分类、3 个难度，4 道题带围栏代码块（验证 Markdown + Shiki 高亮）。
-- ============================================================

USE `codeinterview`;

-- ------------------------------------------------------------
-- 1. 演示账号（admin=站长本人 / demo=演示用户）
-- ------------------------------------------------------------
INSERT IGNORE INTO `user` (`id`, `nickname`, `password`, `role`, `status`, `created_at`, `updated_at`)
VALUES
  (1, 'admin', '$2a$12$6wXRLg1lVfDSVXtrZf/qFOi3ItkYOjVfTFmcK8975z.cIUQo9bbVq', 1, 1, NOW(3), NOW(3)),
  (2, 'demo',  '$2a$12$6wXRLg1lVfDSVXtrZf/qFOi3ItkYOjVfTFmcK8975z.cIUQo9bbVq', 0, 1, NOW(3), NOW(3));

-- ------------------------------------------------------------
-- 2. 演示题目（18 道，全部发布态；id 101-118）
-- ------------------------------------------------------------
INSERT IGNORE INTO `question`
  (`id`, `category_id`, `title`, `content_md`, `answer_md`, `difficulty`, `status`, `source`,
   `view_count`, `practice_count`, `published_at`, `created_by`)
VALUES
  (101, 1, 'String、StringBuilder、StringBuffer 有什么区别？',
   '从**可变性**、**线程安全**、**性能**三个角度回答，并说明各自适用场景。\n\n```java\nString s = "a" + "b";              // 编译期常量折叠\nStringBuilder sb = new StringBuilder(); // 可变，非线程安全\nStringBuffer buf = new StringBuffer();  // 可变，方法同步\n```',
   '**String** 不可变，每次拼接产生新对象，适合作为常量/Map key。\n\n**StringBuilder** 可变、非线程安全，单线程拼接首选。\n\n**StringBuffer** 可变、方法加 `synchronized`，多线程拼接才考虑，现代代码更常用 `StringBuilder` + 局部变量。\n\n注意：循环内 `String +=` 会被编译器优化为 `StringBuilder`，但跨迭代仍可能反复创建对象，循环拼接建议显式用 `StringBuilder`。',
   1, 1, '演示数据', 128, 2, NOW(3) - INTERVAL 1 HOUR, 1),
  (102, 1, 'ArrayList 与 LinkedList 的适用场景有什么不同？',
   '请从底层数据结构、随机访问、头尾插入、内存占用四个维度对比，并给出选型建议。',
   '**ArrayList**：动态数组，随机访问 O(1)，尾部追加均摊 O(1)，中间插入/删除 O(n)，内存连续、缓存友好。\n\n**LinkedList**：双向链表，随机访问 O(n)，头尾插入删除 O(1)，每个节点额外存两个指针，内存开销大、缓存不友好。\n\n**选型**：绝大多数场景用 `ArrayList`；只有频繁在头部插入/删除且不需要随机访问时才考虑 `LinkedList`（实际项目中 `ArrayDeque` 往往更合适）。',
   1, 1, '演示数据', 96, 2, NOW(3) - INTERVAL 2 HOUR, 1),
  (103, 1, 'synchronized 与 ReentrantLock 的区别？',
   '从锁的实现、可中断性、公平性、条件变量、释放方式五个方面对比。\n\n```java\nReentrantLock lock = new ReentrantLock(true); // 公平锁\nlock.lock();\ntry {\n    // critical section\n} finally {\n    lock.unlock(); // 必须手动释放\n}\n```',
   '| 维度 | synchronized | ReentrantLock |\n|---|---|---|\n| 实现 | JVM 内置 monitor | AQS + CAS |\n| 可中断 | 不可 | `lockInterruptibly()` 可 |\n| 公平性 | 非公平 | 可选公平/非公平 |\n| 条件变量 | 一个 wait/notify | 多个 Condition |\n| 释放 | 自动（异常也会释放） | 必须 `finally` 手动 unlock |\n\n**建议**：能用 `synchronized` 就用它（简单、不会忘记释放）；需要超时、可中断、多条件队列再用 `ReentrantLock`。',
   2, 1, '演示数据', 77, 1, NOW(3) - INTERVAL 3 HOUR, 1),
  (104, 1, 'JVM 类加载过程与双亲委派模型？',
   '说明加载、验证、准备、解析、初始化五个阶段，以及双亲委派的作用与打破场景。',
   '**过程**：加载 → 验证 → 准备（静态变量赋零值）→ 解析 → 初始化（执行 `<clinit>`）。\n\n**双亲委派**：类加载请求先交给父加载器，父加载器无法完成才由子加载器加载。作用：避免核心类被篡改、保证类只加载一次。\n\n**打破场景**：SPI（`ServiceLoader` 用线程上下文类加载器）、OSGi 热部署、Tomcat 的 WebAppClassLoader（Web 应用隔离）。',
   2, 1, '演示数据', 64, 1, NOW(3) - INTERVAL 4 HOUR, 1),
  (105, 2, '为什么 MySQL InnoDB 用 B+ 树而不是 B 树或哈希索引？',
   '从磁盘 IO、范围查询、全表扫描三个角度说明 B+ 树的优势。',
   '**磁盘 IO**：B+ 树只有叶子节点存数据，非叶子节点只存索引键，单页能放更多键 → 树更矮，磁盘 IO 更少（3-4 层可覆盖千万级）。\n\n**范围查询**：叶子节点用双向链表相连，范围扫描只需顺序遍历叶子，B 树需要中序回溯。\n\n**全表扫描**：B+ 树叶子就是有序数据链，顺序读友好；哈希索引只支持等值查询，无法范围查询与排序。\n\n代价：写入时需要维护页分裂与合并，因此主键建议用自增，减少随机分裂。',
   2, 1, '演示数据', 210, 2, NOW(3) - INTERVAL 5 HOUR, 1),
  (106, 2, 'MVCC 如何实现可重复读？',
   '说明隐藏列、undo log、ReadView 的关系，以及当前读与快照读的区别。',
   '**隐藏列**：每行有 `DB_TRX_ID`（最后修改事务）、`DB_ROLL_PTR`（回滚指针）、`DB_ROW_ID`。\n\n**undo log**：保存行的历史版本，通过回滚指针串成版本链。\n\n**ReadView**：事务第一次快照读时生成，记录活跃事务列表；沿版本链找到第一个「对当前事务可见」的版本。\n\n**当前读**（`SELECT ... FOR UPDATE` / `UPDATE` / `DELETE`）读最新版本并加锁；**快照读**（普通 SELECT）走 MVCC。RR 隔离级别下 ReadView 在整个事务内复用，所以可重复读。',
   3, 1, '演示数据', 155, 1, NOW(3) - INTERVAL 6 HOUR, 1),
  (107, 2, 'InnoDB 的间隙锁与幻读是什么关系？',
   '说明 Record Lock、Gap Lock、Next-Key Lock，以及它们如何解决幻读。',
   '**Record Lock**：锁住索引记录本身。\n\n**Gap Lock**：锁住索引记录之间的间隙，阻止其他事务在间隙中插入。\n\n**Next-Key Lock** = Record Lock + Gap Lock，InnoDB 在 RR 下默认使用。\n\n**幻读**：同一事务两次范围查询结果行数不同。快照读由 MVCC 解决；当前读（如 `SELECT ... FOR UPDATE`）由 Next-Key Lock 锁住范围与间隙解决。\n\n注意：唯一索引等值命中时 Next-Key Lock 会退化为 Record Lock，间隙锁只在范围条件/非唯一索引下出现。',
   3, 1, '演示数据', 132, 1, NOW(3) - INTERVAL 7 HOUR, 1),
  (108, 3, 'Redis zset 为什么用跳表而不是红黑树？',
   '从实现复杂度、范围查询、并发与内存三个角度说明。',
   '**实现复杂度**：跳表插入/删除只需改指针，代码量远小于红黑树的旋转与平衡。\n\n**范围查询**：跳表定位起点后沿底层链表顺序遍历，天然适合 `ZRANGE`；红黑树中序遍历需要递归/栈。\n\n**并发与内存**：跳表可以无锁化改造（Redis 单线程但实现简单），节点平均 1.33 层指针，内存可控。\n\n**结论**：Redis 追求实现简单 + 范围查询高效，跳表是更务实的选择。',
   2, 1, '演示数据', 88, 0, NOW(3) - INTERVAL 8 HOUR, 1),
  (109, 3, 'Redis RDB 与 AOF 的区别与选型？',
   '对比持久化时机、数据安全性、恢复速度、文件体积，并说明混合持久化。',
   '| 维度 | RDB | AOF |\n|---|---|---|\n| 触发 | 定时/手动 `BGSAVE` | 每次写命令追加 |\n| 安全性 | 可能丢最后一次快照后的数据 | 可配置 `appendfsync everysec`，最多丢 1s |\n| 恢复 | 快（直接加载二进制） | 慢（重放命令） |\n| 体积 | 小 | 大（可 `BGREWRITEAOF` 压缩） |\n\n**推荐**：两者都开；Redis 4.0+ 可用混合持久化（`aof-use-rdb-preamble yes`），重写时以 RDB 头 + AOF 增量，兼顾恢复速度与安全性。',
   2, 1, '演示数据', 71, 1, NOW(3) - INTERVAL 9 HOUR, 1),
  (110, 3, '缓存穿透、击穿、雪崩分别是什么？怎么解决？',
   '分别给出定义、成因与至少两种解决方案。',
   '**穿透**：查询不存在的数据，缓存不命中，请求全部打到 DB。解决：布隆过滤器拦截、缓存空值并设短 TTL、参数校验。\n\n**击穿**：热点 key 过期瞬间大量并发请求打到 DB。解决：互斥锁（只放一个请求回源）、热点 key 逻辑过期不物理过期、提前预热。\n\n**雪崩**：大量 key 同时过期或 Redis 宕机，DB 被压垮。解决：过期时间加随机抖动、多级缓存、Redis 高可用（主从/哨兵）、降级限流。',
   2, 1, '演示数据', 190, 1, NOW(3) - INTERVAL 10 HOUR, 1),
  (111, 4, 'JavaScript 事件循环中宏任务与微任务如何执行？',
   '给出一个包含 setTimeout、Promise、async/await 的输出顺序题并解释。\n\n```javascript\nconsole.log(1)\nsetTimeout(() => console.log(2), 0)\nPromise.resolve().then(() => console.log(3))\nconsole.log(4)\n```',
   '**输出**：`1 4 3 2`。\n\n**原因**：\n1. 同步代码先执行：`1`、`4`；\n2. 当前宏任务结束后清空微任务队列：`Promise.then` 输出 `3`；\n3. 进入下一次事件循环，宏任务 `setTimeout` 输出 `2`。\n\n补充：`async/await` 中 `await` 之后的代码等价于 `Promise.then` 里的微任务；`process.nextTick`（Node）优先于 Promise 微任务。',
   2, 1, '演示数据', 145, 1, NOW(3) - INTERVAL 11 HOUR, 1),
  (112, 4, '从输入 URL 到页面渲染发生了什么？',
   '按 DNS、TCP/TLS、HTTP、浏览器解析与渲染、关键路径五个阶段说明。',
   '1. **URL 解析与 DNS**：缓存（浏览器/系统/路由器/DNS 服务器）逐级查询。\n2. **建立连接**：TCP 三次握手；HTTPS 再加 TLS 握手。\n3. **发起请求**：HTTP 请求、服务端响应（HTML）。\n4. **解析渲染**：HTML → DOM，CSS → CSSOM，合并 Render Tree，布局（Layout），绘制（Paint），合成（Composite）。\n5. **关键路径**：`<script>` 阻塞解析（可 `defer/async`），CSS 阻塞渲染；首屏优化围绕减少关键资源、避免布局抖动展开。',
   2, 1, '演示数据', 166, 0, NOW(3) - INTERVAL 12 HOUR, 1),
  (113, 4, 'Vue 3 响应式原理与 Vue 2 有什么不同？',
   '说明 Proxy 与 Object.defineProperty 的差异，以及依赖收集的 trigger/track。\n\n```javascript\nconst state = reactive({ count: 0 })\neffect(() => console.log(state.count))\nstate.count++ // 触发 effect 重新执行\n```',
   '**Vue 2**：`Object.defineProperty` 递归劫持每个属性，无法监听新增/删除属性与数组下标，需要 `$set`/`$delete` 补丁。\n\n**Vue 3**：`Proxy` 代理整个对象，天然支持新增/删除、数组索引与 `length`；`track` 在 getter 中收集依赖，`trigger` 在 setter 中派发更新。\n\n**额外收益**：懒代理（嵌套对象访问时才代理）、更好的 TS 支持、`ref`/`reactive` 两套 API。',
   3, 1, '演示数据', 203, 0, NOW(3) - INTERVAL 13 HOUR, 1),
  (114, 4, '前端首屏优化有哪些手段？',
   '从网络、资源、渲染、运行时四个层面各举两例，并说明度量指标。',
   '**网络**：CDN、HTTP/2 多路复用、预连接 `preconnect`、gzip/brotli。\n\n**资源**：代码分割与懒加载、图片压缩/WebP、字体子集化、第三方脚本延迟加载。\n\n**渲染**：SSR/预渲染、关键 CSS 内联、避免布局抖动（固定尺寸）、骨架屏。\n\n**运行时**：减少长任务、虚拟列表、`requestIdleCallback` 分片。\n\n**度量**：LCP、FCP、TTFB、CLS、INP，可用 Lighthouse/Performance API 采集。',
   1, 1, '演示数据', 57, 0, NOW(3) - INTERVAL 14 HOUR, 1),
  (115, 5, '进程与线程的区别？协程又是什么？',
   '从资源归属、调度、切换开销、通信方式四个维度对比。',
   '**进程**：资源分配的最小单位，拥有独立地址空间；进程间通信需管道/共享内存/消息队列。\n\n**线程**：CPU 调度的最小单位，同一进程内线程共享地址空间，切换开销小于进程，需处理同步问题。\n\n**协程**：用户态轻量级线程，由程序自己调度，切换不陷内核，适合 IO 密集高并发（Go goroutine、Java 虚拟线程）。\n\n**一句话**：进程隔离、线程共享、协程轻量。',
   1, 1, '演示数据', 118, 0, NOW(3) - INTERVAL 15 HOUR, 1),
  (116, 5, '死锁的四个必要条件是什么？如何预防？',
   '列出四个条件，并给出至少三种破坏死锁的方法，说明 Java 中的实践。',
   '**四个必要条件**：互斥、占有且等待、不可抢占、循环等待。\n\n**破坏方式**：\n- 破坏「占有且等待」：一次性申请全部资源，或申请失败释放已持有资源；\n- 破坏「不可抢占」：可抢占资源；\n- 破坏「循环等待」：给锁定义全局顺序，按顺序加锁（最常用）。\n\n**Java 实践**：固定加锁顺序；用 `ReentrantLock.tryLock(timeout)` 避免无限等待；用 `jstack` 检测死锁（`Found one Java-level deadlock`）。',
   2, 1, '演示数据', 99, 0, NOW(3) - INTERVAL 16 HOUR, 1),
  (117, 5, '虚拟内存与分页机制解决了什么问题？',
   '说明虚拟地址到物理地址的转换、页表、缺页中断与页面置换。',
   '**解决的问题**：进程隔离、内存超售（虚拟地址空间可大于物理内存）、按需加载（局部性原理）。\n\n**转换**：虚拟地址 → 页号 + 页内偏移 → 页表查物理页框 → 物理地址；TLB 缓存加速。\n\n**缺页中断**：访问的页不在内存时触发，从磁盘换入；内存满时按置换算法（LRU/Clock）换出。\n\n**代价**：多级页表、TLB 命中率、缺页率决定性能；大页可减少页表项与 TLB miss。',
   3, 1, '演示数据', 84, 0, NOW(3) - INTERVAL 17 HOUR, 1),
  (118, 5, 'select/poll/epoll 的区别？epoll 为什么高效？',
   '对比三者的数据结构、时间复杂度与适用场景，说明水平触发与边缘触发。',
   '| 维度 | select | poll | epoll |\n|---|---|---|---|\n| 数据结构 | fd 位图（1024 上限） | pollfd 数组 | 红黑树 + 就绪链表 |\n| 每次调用 | 拷贝全部 fd | 拷贝全部 fd | 只拷贝一次（`epoll_ctl` 注册） |\n| 时间复杂度 | O(n) | O(n) | O(就绪数) |\n\n**epoll 高效原因**：内核用红黑树管理 fd，事件就绪时加入就绪链表，`epoll_wait` 只返回就绪 fd，无需遍历全部。\n\n**LT/ET**：水平触发（默认）只要可读写就持续通知；边缘触发只在状态变化时通知一次，必须配合非阻塞 IO 循环读到 `EAGAIN`。',
   3, 1, '演示数据', 143, 0, NOW(3) - INTERVAL 18 HOUR, 1);

-- ------------------------------------------------------------
-- 3. 演示题目标签
-- ------------------------------------------------------------
INSERT IGNORE INTO `question_tag` (`question_id`, `tag_id`) VALUES
  (101, 1), (101, 12),
  (102, 1), (102, 11),
  (103, 2), (103, 7),
  (104, 3), (104, 4),
  (105, 5), (105, 11),
  (106, 6),
  (107, 7),
  (108, 9), (108, 11),
  (109, 10),
  (110, 9),
  (111, 12), (111, 13),
  (112, 13),
  (113, 14), (113, 12),
  (114, 12),
  (115, 15),
  (116, 15),
  (117, 15),
  (118, 15);

-- ------------------------------------------------------------
-- 4. demo 用户的自评 / 错题 / 收藏（让统计页与图表非空）
-- ------------------------------------------------------------
INSERT IGNORE INTO `practice_record` (`id`, `user_id`, `question_id`, `self_assess`, `source`, `created_at`) VALUES
  (1001, 1, 101, 1, 1, NOW(3) - INTERVAL 50 MINUTE),
  (1002, 1, 102, 2, 1, NOW(3) - INTERVAL 40 MINUTE),
  (1003, 1, 102, 2, 1, NOW(3) - INTERVAL 30 MINUTE),
  (1004, 1, 103, 3, 1, NOW(3) - INTERVAL 26 HOUR),
  (1005, 1, 104, 1, 1, NOW(3) - INTERVAL 25 HOUR),
  (1006, 1, 105, 3, 1, NOW(3) - INTERVAL 50 HOUR),
  (1007, 1, 106, 2, 1, NOW(3) - INTERVAL 49 HOUR),
  (1008, 1, 107, 1, 3, NOW(3) - INTERVAL 74 HOUR),
  (1009, 1, 108, 2, 1, NOW(3) - INTERVAL 98 HOUR),
  (1010, 1, 109, 1, 1, NOW(3) - INTERVAL 122 HOUR),
  (1011, 1, 110, 3, 1, NOW(3) - INTERVAL 146 HOUR),
  (1012, 1, 111, 2, 1, NOW(3) - INTERVAL 145 HOUR),
  (1013, 1, 111, 1, 1, NOW(3) - INTERVAL 120 HOUR),
  (1014, 2, 101, 2, 1, NOW(3) - INTERVAL 45 MINUTE),
  (1015, 2, 105, 1, 1, NOW(3) - INTERVAL 35 MINUTE),
  (1016, 2, 107, 3, 1, NOW(3) - INTERVAL 28 HOUR),
  (1017, 2, 112, 1, 1, NOW(3) - INTERVAL 52 HOUR);

INSERT IGNORE INTO `wrong_record`
  (`id`, `user_id`, `question_id`, `last_assess`, `wrong_count`, `first_wrong_at`, `last_wrong_at`,
   `resolved`, `mastered_at`, `created_at`, `updated_at`)
VALUES
  (1001, 1, 102, 2, 2, NOW(3) - INTERVAL 40 MINUTE, NOW(3) - INTERVAL 30 MINUTE, 0, NULL, NOW(3), NOW(3)),
  (1002, 1, 103, 3, 1, NOW(3) - INTERVAL 26 HOUR, NOW(3) - INTERVAL 26 HOUR, 0, NULL, NOW(3), NOW(3)),
  (1003, 1, 105, 3, 1, NOW(3) - INTERVAL 50 HOUR, NOW(3) - INTERVAL 50 HOUR, 0, NULL, NOW(3), NOW(3)),
  (1004, 1, 108, 2, 1, NOW(3) - INTERVAL 98 HOUR, NOW(3) - INTERVAL 98 HOUR, 0, NULL, NOW(3), NOW(3)),
  (1005, 1, 110, 3, 1, NOW(3) - INTERVAL 146 HOUR, NOW(3) - INTERVAL 146 HOUR, 0, NULL, NOW(3), NOW(3)),
  (1006, 1, 111, 1, 2, NOW(3) - INTERVAL 145 HOUR, NOW(3) - INTERVAL 120 HOUR, 1, NOW(3) - INTERVAL 120 HOUR, NOW(3), NOW(3)),
  (1007, 2, 101, 2, 1, NOW(3) - INTERVAL 45 MINUTE, NOW(3) - INTERVAL 45 MINUTE, 0, NULL, NOW(3), NOW(3)),
  (1008, 2, 107, 3, 1, NOW(3) - INTERVAL 28 HOUR, NOW(3) - INTERVAL 28 HOUR, 0, NULL, NOW(3), NOW(3));

INSERT IGNORE INTO `user_favorite` (`id`, `user_id`, `question_id`, `created_at`) VALUES
  (1001, 1, 101, NOW(3) - INTERVAL 2 DAY),
  (1002, 1, 104, NOW(3) - INTERVAL 1 DAY),
  (1003, 1, 107, NOW(3) - INTERVAL 3 HOUR),
  (1004, 2, 102, NOW(3) - INTERVAL 2 HOUR);

-- ------------------------------------------------------------
-- 5. 题目练习次数与演示自评对齐（热度榜/统计口径）
-- ------------------------------------------------------------
UPDATE `question` SET `practice_count` = 2 WHERE `id` = 101;
UPDATE `question` SET `practice_count` = 2 WHERE `id` = 102;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 103;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 104;
UPDATE `question` SET `practice_count` = 2 WHERE `id` = 105;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 106;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 107;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 108;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 109;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 110;
UPDATE `question` SET `practice_count` = 2 WHERE `id` = 111;
UPDATE `question` SET `practice_count` = 1 WHERE `id` = 112;
