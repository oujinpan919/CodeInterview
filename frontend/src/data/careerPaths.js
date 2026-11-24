/**
 * 实习 / 校招岗位刷题路线（v4.16）。
 *
 * 为什么放在前端静态数据而不是数据库：
 * 1. 这批内容是「通用求职方法论 + 题单编排」，不是用户数据，也不需要后台编辑；
 * 2. 需要整块卡片渲染（JD 拆解 / 阶段 / 题单 / 项目 / 面试 / 里程碑），
 *    后端 learning-path 表只有 name + description，撑不起这个结构；
 * 3. 改动走前端发版即可，不引入新的后端表、接口和权限面。
 *
 * 内容口径：按当前市面实习 / 初级岗位 JD 的常见要求归纳，
 * 不绑定任何具体公司，也不写死薪资与招聘时间（这类信息变化快、易过期）。
 * 所有页面文案都会带「参考当前招聘 JD，建议定期更新」的提示。
 */

/** 难度分布用「简单 / 中等 / 困难」比例表示，便于页面画条形图 */
const D = (easy, medium, hard) => ({ easy, medium, hard })

export const CAREER_PATHS = [
  {
    id: 'frontend',
    name: '前端开发',
    shortName: '前端',
    tagline: '页面还原 + 交互实现 + 工程化',
    months: '3–4 个月',
    weeklyHours: '每周 12–15 小时',
    targetRoles: ['前端开发实习生', 'Web 前端工程师（初级）', '大前端 / 跨端开发实习生'],
    scenario:
      '实习面试通常两个环节：一面考 JS 基础、CSS 布局、手写代码；二面考框架原理、工程化和项目深挖。' +
      '前端相对友好的一点是「能跑起来的东西就能讲」——有一个真的能打开的项目，胜过多背十道八股。',
    jd: [
      {
        area: '编程语言',
        items: [
          'JavaScript 必熟：原型链、闭包、this、事件循环（宏任务/微任务）、Promise',
          'TypeScript 已成主流要求：泛型、联合类型、类型收窄、工具类型',
          'ES6+ 语法：解构、展开、箭头函数、模块化（ESM / CJS 差异）'
        ]
      },
      {
        area: '计算机基础',
        items: [
          'HTTP / HTTPS、状态码、缓存（强缓存/协商缓存）、跨域与 CORS',
          '浏览器渲染流程：解析 → 构建 DOM/CSSOM → 布局 → 绘制 → 合成',
          'TCP 三次握手 / 四次挥手（前端问得不深但基本必问）'
        ]
      },
      {
        area: '框架与工具',
        items: [
          'Vue：响应式原理（Proxy vs defineProperty）、生命周期、diff、computed/watch 区别',
          'React：Hooks 规则与依赖数组、虚拟 DOM、Fiber、状态管理选型',
          '构建工具：Vite / Webpack 的构建流程、HMR、打包优化（分包、Tree Shaking）',
          'CSS：Flex / Grid、BFC、盒模型、层叠上下文、移动端适配（vw/rem/媒体查询）'
        ]
      },
      {
        area: '项目经验',
        items: [
          '至少 1 个可公网访问的完整项目，含真实数据交互而非纯静态页',
          '能说清：为什么这么选型、遇到过什么坑、怎么定位和解决',
          '有性能优化或工程化实践（懒加载、代码分割、Lighthouse 分数）'
        ]
      },
      {
        area: '算法与数据结构',
        items: [
          '中等难度为主：数组、字符串、哈希表、双指针、二叉树、简单 DP',
          '前端特有：手写防抖节流、深拷贝、Promise 系列、数组扁平化、call/apply/bind',
          '复杂度分析要能开口就讲'
        ]
      },
      {
        area: '八股与面经',
        items: [
          'JS 基础 + 浏览器 + 网络 是三大高频块',
          '框架原理题在二面集中出现',
          '项目追问会问到具体 API 和边界情况'
        ]
      },
      {
        area: '工程能力',
        items: ['Git 分支协作与冲突解决', '代码规范（ESLint / Prettier）', '单元测试意识', 'CI/CD 基本概念']
      }
    ],
    stages: [
      {
        key: 'foundation',
        name: '基础补齐',
        weeks: '第 1–5 周',
        goal: '把 JS / CSS / HTTP 三条线补齐到能看懂框架源码注释的程度',
        weeklyTasks: [
          'JS 基础专题 12 个知识点，每个配 3 道题',
          'CSS 布局手写 8 个常见布局（居中、三栏、圣杯、等高、粘性页脚等）',
          'HTTP 缓存与跨域各整理一篇笔记'
        ],
        deliverable: '一份自己的 JS 知识脑图 + 8 个布局 demo'
      },
      {
        key: 'drill',
        name: '专项刷题',
        weeks: '第 6–10 周',
        goal: '手写题形成肌肉记忆，算法题稳定做出中等难度',
        weeklyTasks: ['手写代码专题 15 道（防抖/深拷贝/Promise 等）', '算法题每周 10 道（中等为主）', '框架原理专题 6 个']
      },
      {
        key: 'project',
        name: '项目实战',
        weeks: '第 11–14 周',
        goal: '做出一个能公网访问、有真实数据流的前端项目',
        weeklyTasks: ['完成核心功能开发', '接入真实接口 / 自建 mock 服务', '做一轮性能优化并记录前后对比数据'],
        deliverable: '可访问的项目 + README + 一段 3 分钟的项目讲解稿'
      },
      {
        key: 'sprint',
        name: '面试冲刺',
        weeks: '第 15–16 周',
        goal: '把所有考点串成能连续输出 40 分钟的表达',
        weeklyTasks: ['每天 1 次自问自答录音', '高频八股二轮复习', '模拟面试 3 次（找同学或自己录屏）'],
        deliverable: '一份项目追问 Q&A 清单（至少 20 问）'
      }
    ],
    questionList: [
      {
        topic: 'JavaScript 基础',
        category: 'JavaScript 面试题',
        count: 40,
        difficulty: D(14, 20, 6),
        hot: '原型链与继承、闭包与内存、this 绑定、事件循环输出题',
        handWrite: '手写 new、call/apply/bind、instanceof'
      },
      {
        topic: 'CSS 与布局',
        category: 'JavaScript 面试题',
        count: 15,
        difficulty: D(7, 6, 2),
        hot: 'Flex vs Grid 选型、BFC、层叠上下文、移动端 1px 问题',
        handWrite: '手写水平垂直居中 4 种、三栏布局、响应式卡片流'
      },
      {
        topic: '浏览器与网络',
        category: '计算机网络面试题',
        count: 25,
        difficulty: D(9, 12, 4),
        hot: '输入 URL 到渲染、强缓存与协商缓存、跨域方案对比、HTTP/2 多路复用',
        handWrite: '手写简易 Promise、Promise.all / race'
      },
      {
        topic: 'Vue / React 框架原理',
        category: 'Vue 基础面试题',
        count: 20,
        difficulty: D(5, 11, 4),
        hot: '响应式实现差异、diff 算法、Hooks 依赖陷阱、组件通信方式',
        handWrite: '手写 reactive 简化版、实现一个简易 diff'
      },
      {
        topic: '算法与数据结构',
        category: '数据结构面试题',
        count: 60,
        difficulty: D(20, 30, 10),
        hot: '数组双指针、链表反转、二叉树遍历、哈希表计数、简单 DP',
        handWrite: '手写快排、归并排序、二分查找边界'
      },
      {
        topic: '工程化与性能',
        category: 'JavaScript 面试题',
        count: 15,
        difficulty: D(5, 8, 2),
        hot: 'Vite 为什么快、Tree Shaking 条件、首屏优化手段、打包体积治理',
        handWrite: '手写一个简易打包配置 / 性能监控上报'
      }
    ],
    portfolio: {
      projects: [
        '一个有真实后端或 mock 服务的 Web 应用（如题库、待办、数据看板）',
        '一个组件库或工具函数库（能说明设计取舍）'
      ],
      description: [
        '用「背景 - 我的角色 - 技术选型 - 难点 - 结果」五段式写，控制在 200 字内',
        '难点要具体到可验证：例如「首屏 3.2s → 1.1s，主要靠路由懒加载 + 图片压缩 + 骨架屏」',
        '能量化就量化：页面数、组件数、接口数、性能提升幅度'
      ],
      highlights: [
        '我为什么选这个技术栈，替代方案是什么，放弃的原因',
        '我解决的一个真实 bug：现象 → 排查路径 → 根因 → 修复 → 回归',
        '一次性能或工程化优化，有前后对比数据'
      ],
      followUps: [
        '这个项目如果用户量涨 100 倍，哪里先崩？',
        '你的状态管理为什么这么设计？跨组件通信怎么处理？',
        '首屏优化具体做了什么？怎么测的？',
        '如果让你重做，会改哪三处？'
      ],
      checklist: [
        '能不看代码画出项目架构图',
        '能解释每一个依赖为什么存在',
        '能说出至少 2 个踩过的坑和修复方式',
        '项目已部署且链接可访问',
        'README 有截图、运行方式和难点说明'
      ]
    },
    milestones: [
      { text: '手写题 15 道全部能一次写对（防抖/深拷贝/Promise 等）', weight: '手写' },
      { text: '算法中等题 60 道完成，稳定 25 分钟内做出', weight: '算法' },
      { text: '能独立讲清 1 个项目的完整技术方案与 2 个难点', weight: '项目' },
      { text: '完成 3 次模拟面试，每次能连续输出 40 分钟', weight: '面试' }
    ]
  },

  {
    id: 'backend',
    name: '后端开发',
    shortName: '后端',
    tagline: '语言基础 + 数据库 + 框架原理 + 分布式认知',
    months: '4–5 个月',
    weeklyHours: '每周 14–18 小时',
    targetRoles: ['Java 后端开发实习生', '服务端开发工程师（初级）', 'Go / Node 后端实习生（视团队技术栈）'],
    scenario:
      '后端实习面试是「基础密度最高」的方向：一面几乎全是计算机基础与语言细节，二面开始问框架原理、' +
      '数据库设计和项目，三面看你对分布式与系统设计的初步认知。投递前最好有一个真的能跑的后端服务。',
    jd: [
      {
        area: '编程语言',
        items: [
          'Java 主线：集合源码、并发（线程池/锁/可见性）、JVM 内存与 GC',
          '语言细节会被追问到源码级：HashMap 扩容、AQS、ThreadLocal 内存泄漏',
          '部分岗位要求 Go 或 C++，但基础概念（内存模型、GC）是相通的'
        ]
      },
      {
        area: '计算机基础',
        items: [
          '操作系统：进程/线程、调度、内存管理、虚拟内存、IO 多路复用',
          '计算机网络：TCP 状态机、拥塞控制、HTTP/HTTPS、DNS 解析',
          '数据结构：树、哈希、堆、并查集的实际应用场景'
        ]
      },
      {
        area: '框架与工具',
        items: [
          'Spring：IoC 容器启动流程、AOP 代理方式、事务传播与失效场景、循环依赖三级缓存',
          'MyBatis / JPA：SQL 映射、一级二级缓存、动态 SQL',
          '中间件：Redis 数据结构与持久化、消息队列（Kafka/RocketMQ）基本模型',
          'MySQL：索引结构（B+ 树）、执行计划、事务隔离级别、MVCC、锁'
        ]
      },
      {
        area: '项目经验',
        items: [
          '至少 1 个有真实业务逻辑的后端服务，含鉴权、分页、事务、缓存',
          '能说清表结构设计与索引选择的理由',
          '有接口性能优化或并发问题处理的经历'
        ]
      },
      {
        area: '算法与数据结构',
        items: [
          '中等难度为主，后端更看重复杂度分析是否清晰',
          '高频：链表、二叉树、哈希、双指针、回溯、简单动态规划',
          '部分公司会考 SQL 手写（多表 join、窗口函数）'
        ]
      },
      {
        area: '八股与面经',
        items: ['Java 基础 / 并发 / JVM 三大块是重灾区', 'MySQL 与 Redis 几乎必问', 'Spring 原理在二面高频']
      },
      {
        area: '工程能力',
        items: ['Linux 常用命令与排查（top / grep / tail / netstat）', 'Git 协作', '日志与异常处理规范', '接口设计（RESTful、幂等、限流）']
      }
    ],
    stages: [
      {
        key: 'foundation',
        name: '基础补齐',
        weeks: '第 1–6 周',
        goal: 'Java 基础 + 计网 + 操作系统三条线打穿到能讲清原理',
        weeklyTasks: [
          'Java 基础与集合源码每周 2 个专题',
          '操作系统与网络各 3 个高频专题',
          '每专题输出一段 3 分钟口述稿'
        ],
        deliverable: '一份自建的「八股口述稿」文档，覆盖 30 个高频题'
      },
      {
        key: 'drill',
        name: '专项刷题',
        weeks: '第 7–12 周',
        goal: '并发/JVM/MySQL/Redis 四块专项成型，算法保持手感',
        weeklyTasks: ['并发与 JVM 每周各 1 个专题', 'MySQL + Redis 合计每周 8 道', '算法每周 8 道'],
        deliverable: '四大专项的笔记 + 每个专项 10 道自测题'
      },
      {
        key: 'project',
        name: '项目实战',
        weeks: '第 13–17 周',
        goal: '完成一个含鉴权、事务、缓存的后端服务并能部署',
        weeklyTasks: ['核心接口开发与联调', '补齐异常处理、参数校验、日志', '做一次压测并记录 QPS / 响应时间'],
        deliverable: '可访问的服务 + 表设计文档 + 接口文档'
      },
      {
        key: 'sprint',
        name: '面试冲刺',
        weeks: '第 18–20 周',
        goal: '高频八股形成条件反射，项目追问准备到 30 问',
        weeklyTasks: ['每天 5 道高频八股口述', '项目追问清单扩到 30 问', '模拟面试 3 次'],
        deliverable: '一份 30 问的项目追问 Q&A'
      }
    ],
    questionList: [
      {
        topic: 'Java 基础与集合',
        category: 'Java 基础面试题',
        count: 45,
        difficulty: D(15, 22, 8),
        hot: 'HashMap 原理与扩容、equals/hashCode 契约、String 不可变、泛型擦除、反射',
        handWrite: '手写单例（含双重检查）、手写 LRU、手写生产者消费者'
      },
      {
        topic: '并发编程',
        category: 'Java 并发面试题',
        count: 45,
        difficulty: D(10, 24, 11),
        hot: 'synchronized 与 Lock 对比、AQS、线程池参数与拒绝策略、 volatile、CAS 与 ABA、ThreadLocal',
        handWrite: '手写线程池简化版、手写两个线程交替打印、手写阻塞队列'
      },
      {
        topic: 'JVM',
        category: 'Java 虚拟机面试题',
        count: 40,
        difficulty: D(10, 20, 10),
        hot: '内存区域划分、对象创建与内存布局、GC 算法与收集器、类加载过程与双亲委派、OOM 排查',
        handWrite: '无（偏理论），但要求能画内存结构图并讲清一次 GC 过程'
      },
      {
        topic: 'MySQL',
        category: 'MySQL 面试题',
        count: 50,
        difficulty: D(12, 26, 12),
        hot: 'B+ 树与索引失效、执行计划、事务隔离与 MVCC、间隙锁、主从复制',
        handWrite: '手写多表 join 查询、慢 SQL 优化分析、分页深翻优化'
      },
      {
        topic: 'Redis',
        category: 'Redis 面试题',
        count: 45,
        difficulty: D(12, 22, 11),
        hot: '五大基本类型底层结构、持久化 RDB/AOF、过期与淘汰、缓存穿透/击穿/雪崩、分布式锁',
        handWrite: '手写基于 Redis 的分布式锁（含续期思考）'
      },
      {
        topic: 'Spring 与框架',
        category: 'Spring 面试题',
        count: 40,
        difficulty: D(10, 20, 10),
        hot: 'IoC 启动流程、AOP 两种代理、事务失效场景、循环依赖三级缓存、SpringBoot 自动装配',
        handWrite: '手写简易 IoC 容器'
      },
      {
        topic: '操作系统与网络',
        category: '操作系统面试题',
        count: 40,
        difficulty: D(14, 19, 7),
        hot: '进程线程协程、死锁四条件、虚拟内存与页面置换、IO 多路复用、TCP 状态机',
        handWrite: '无（概念为主）'
      },
      {
        topic: '算法与数据结构',
        category: '数据结构面试题',
        count: 70,
        difficulty: D(22, 36, 12),
        hot: '链表、二叉树、哈希、双指针、回溯、背包与简单 DP',
        handWrite: '手写快排、归并、堆排序、二分边界'
      },
      {
        topic: '系统设计与中间件',
        category: '后端系统设计面试题',
        count: 30,
        difficulty: D(5, 15, 10),
        hot: '消息队列削峰与不丢不重、分布式事务方案、限流熔断、短链/秒杀设计',
        handWrite: '无，但要能白板画架构图'
      }
    ],
    portfolio: {
      projects: ['一个含登录鉴权、分页查询、写操作事务的后端服务（题库/商城/预约均可）', '有缓存、有异步任务的小系统'],
      description: [
        '先讲业务背景与规模（几张表、多少接口、多少 QPS），再讲技术方案',
        '表结构设计要讲得出「为什么这么拆」「索引为什么建在这几列」',
        '写清楚一次真实的性能优化：慢 SQL → 执行计划 → 加索引/改查询 → 耗时对比'
      ],
      highlights: [
        '一次并发问题的定位与解决（超卖、幂等、重复消费）',
        '一次缓存设计：缓存什么、怎么更新、怎么防穿透',
        '接口设计规范：统一响应、错误码、参数校验、幂等键'
      ],
      followUps: [
        '你这个接口的 QPS 大概多少？瓶颈在哪？',
        '事务边界怎么划的？跨服务怎么办？',
        '缓存的更新策略是什么？为什么不先删缓存再更新库？',
        '如果消息重复消费，你怎么保证幂等？',
        '表数据涨到千万级，查询会怎么变化？'
      ],
      checklist: [
        '能画出完整的分层架构图与数据库 ER 图',
        '能说清每条 SQL 是否走索引',
        '能解释服务怎么部署、怎么启动、日志在哪',
        '有接口文档（Swagger 或 markdown）',
        '能说出 2 个以上真实踩坑'
      ]
    },
    milestones: [
      { text: 'Java 基础/并发/JVM 三大块各完成 30 道高频题', weight: '八股' },
      { text: '算法中等题 70 道完成，稳定 30 分钟内做出', weight: '算法' },
      { text: '能独立讲清 1 个后端服务的架构、表设计与 3 个技术难点', weight: '项目' },
      { text: '项目追问能答到 30 问不卡壳', weight: '面试' }
    ]
  },

  {
    id: 'algorithm',
    name: '算法 / 数据方向',
    shortName: '算法',
    tagline: '算法硬实力 + 数学基础 + 数据工程能力',
    months: '5–6 个月',
    weeklyHours: '每周 18–22 小时',
    targetRoles: ['算法工程师实习生（推荐/搜索/NLP/CV）', '数据分析实习生', '数据开发实习生'],
    scenario:
      '算法岗门槛最高、准备周期最长，且「算法题 + 项目深度」两者权重都高。' +
      '数据方向（数据分析 / 数据开发）相对友好：SQL 与业务理解权重高，算法要求低于算法工程师。' +
      '如果时间紧，建议先确认自己投的是哪一类，两条路线的刷题重心不同。',
    jd: [
      {
        area: '编程语言',
        items: [
          'Python 为绝对主力：数据结构、迭代器/生成器、装饰器、面向对象',
          '需要熟练使用 NumPy / Pandas 做数据处理',
          '部分岗位要求 C++（工程落地、性能敏感）'
        ]
      },
      {
        area: '数学与统计',
        items: [
          '概率论：条件概率、贝叶斯、常见分布、期望方差',
          '线性代数：矩阵运算、特征值、SVD',
          '统计：假设检验、置信区间、AB 实验设计（数据方向必问）'
        ]
      },
      {
        area: '机器学习基础',
        items: [
          '经典模型：线性/逻辑回归、决策树、随机森林、GBDT/XGBoost、SVM',
          '评估指标：精确率/召回率/F1、AUC、ROC、mAP、RMSE',
          '过拟合与正则化、交叉验证、特征工程',
          '深度学习（算法工程师必备）：反向传播、优化器、Transformer 基本结构'
        ]
      },
      {
        area: '数据与工具',
        items: [
          'SQL 必须熟练到能写窗口函数、多表关联、复杂聚合',
          'Hive / Spark 基本概念（数据开发岗要求更细）',
          '数据可视化与指标体系建设（数据方向）'
        ]
      },
      {
        area: '项目与竞赛',
        items: [
          '至少 1 个完整项目：数据获取 → 特征 → 模型 → 评估 → 结论',
          '有 Kaggle / 天池类竞赛经历是明显加分',
          '数据方向：一份真实的分析报告比模型更重要'
        ]
      },
      {
        area: '算法与数据结构',
        items: [
          '算法工程师：中等偏难，DP、图、字符串、树都要熟',
          '数据方向：中等为主，数组、哈希、排序、SQL 题占比高',
          '手写要求：能白板写出完整可运行代码'
        ]
      },
      {
        area: '八股与面经',
        items: ['机器学习八股是核心：模型原理、损失函数、推导', '项目里的每个技术选择都会被追问', '数据方向会问业务指标拆解']
      },
      {
        area: '工程能力',
        items: ['Git', 'Linux 与脚本', '模型部署基本认知（离线/在线）', '实验管理与可复现性']
      }
    ],
    stages: [
      {
        key: 'foundation',
        name: '基础补齐',
        weeks: '第 1–8 周',
        goal: '数学 + 机器学习经典模型 + Python 工程能力三项到位',
        weeklyTasks: ['每周 2 个经典模型（原理 + 手推 + 调包实现）', '数学专题每周 1 个', 'Python 数据处理练习'],
        deliverable: '一份模型手推笔记，覆盖 10 个经典模型'
      },
      {
        key: 'drill',
        name: '专项刷题',
        weeks: '第 9–16 周',
        goal: '算法题达到中等稳定、难题有思路；SQL 熟练',
        weeklyTasks: ['算法每周 12 道（中等为主，逐步加难）', 'SQL 每周 10 道', '机器学习八股每周 15 题'],
        deliverable: '算法题解笔记 150 道 + SQL 题单 80 道'
      },
      {
        key: 'project',
        name: '项目实战',
        weeks: '第 17–22 周',
        goal: '完成一个端到端的算法或数据分析项目',
        weeklyTasks: ['数据获取与清洗', '特征工程与模型迭代', '写实验报告与结论'],
        deliverable: '项目代码 + 实验报告 + 一份 5 页的项目讲解文档'
      },
      {
        key: 'sprint',
        name: '面试冲刺',
        weeks: '第 23–26 周',
        goal: '把项目讲到能扛住三层追问，八股形成体系',
        weeklyTasks: ['白板手写算法每周 10 道', '模型原理口述', '模拟面试 4 次'],
        deliverable: '20 问项目追问清单 + 模型原理速查卡'
      }
    ],
    questionList: [
      {
        topic: '算法与数据结构',
        category: '数据结构面试题',
        count: 120,
        difficulty: D(30, 60, 30),
        hot: '动态规划、图论（DFS/BFS/最短路）、二叉树、堆与优先队列、并查集、字符串匹配',
        handWrite: '手写快排/归并/堆排、手写 LRU、手写 TopK、手写并查集'
      },
      {
        topic: '机器学习八股',
        category: '算法和数据结构面试题',
        count: 60,
        difficulty: D(15, 30, 15),
        hot: '偏差方差、过拟合处理、XGBoost 与 GBDT 区别、Transformer 注意力、损失函数选型',
        handWrite: '手推逻辑回归梯度、手写 KMeans、手写简单梯度下降'
      },
      {
        topic: 'SQL 与数据处理',
        category: 'MySQL 面试题',
        count: 50,
        difficulty: D(15, 25, 10),
        hot: '窗口函数、多表 join、分组聚合、连续登录、留存率、同比环比',
        handWrite: '手写留存率查询、手写连续 N 天登录、手写分组 TopN'
      },
      {
        topic: '数学与统计',
        category: null,
        count: 30,
        difficulty: D(10, 14, 6),
        hot: '贝叶斯应用、常见分布、AB 实验样本量、假设检验第一二类错误',
        handWrite: '手推贝叶斯公式、手算假设检验'
      },
      {
        topic: '计算机基础',
        category: '操作系统面试题',
        count: 25,
        difficulty: D(10, 11, 4),
        hot: '进程线程、内存管理、Linux 常用命令、Python GIL',
        handWrite: '无'
      }
    ],
    portfolio: {
      projects: [
        '算法方向：一个完整的建模项目（推荐/分类/序列预测），有 baseline 对比',
        '数据方向：一份真实业务数据分析报告（指标体系 + 结论 + 建议）'
      ],
      description: [
        '算法项目要写清：数据规模、特征数量、baseline 指标、最终指标、提升来自哪里',
        '一定要有对照组：不用复杂模型的 baseline 是多少，说明改进的边际收益',
        '数据报告要写清：业务问题 → 指标拆解 → 分析过程 → 结论 → 可执行建议'
      ],
      highlights: [
        '一次指标提升的完整归因：是特征还是模型还是数据质量问题',
        '一次踩坑：数据泄漏、样本不均衡、评估指标选错',
        '对业务的理解：这个模型上线后怎么用、怎么评估线上效果'
      ],
      followUps: [
        '为什么选这个模型？换一个会怎样？',
        '你的特征是怎么构造的？有没有做特征筛选？',
        '指标提升是真实提升还是过拟合？怎么验证的？',
        '如果数据分布变了，模型失效怎么办？',
        '这个项目如果上线，你会怎么监控？'
      ],
      checklist: [
        '能讲清每一个特征的含义和构造方式',
        '能解释评估指标为什么选这个而不是另一个',
        '实验可复现（有随机种子、有环境说明）',
        '有 baseline 对比数据',
        '能画出完整的数据流与建模流程图'
      ]
    },
    milestones: [
      { text: '算法中等题 60 道 + 困难题 30 道完成', weight: '算法' },
      { text: '经典机器学习模型 10 个能脱稿推导核心公式', weight: '理论' },
      { text: 'SQL 题单 80 道完成，窗口函数熟练', weight: '数据' },
      { text: '能完整讲清 1 个项目的数据流、建模过程与指标提升归因', weight: '项目' }
    ]
  },

  {
    id: 'qa',
    name: '测试开发',
    shortName: '测开',
    tagline: '测试思维 + 自动化能力 + 编码不弱于后端',
    months: '3–4 个月',
    weeklyHours: '每周 12–16 小时',
    targetRoles: ['测试开发实习生', '自动化测试工程师（初级）', '质量保障 / QA 工程师'],
    scenario:
      '测试开发是「性价比很高」的方向：竞争比后端小，但对编码仍有要求（能写自动化脚本）。' +
      '面试通常考：测试用例设计 + 编程基础 + 自动化框架 + 一些后端基础。' +
      '很多人误以为测开不用写代码，实际上手写代码题一样会考，只是难度略低。',
    jd: [
      {
        area: '编程语言',
        items: [
          'Python 最常用（pytest / requests / selenium），部分团队用 Java',
          '要求能独立写自动化测试脚本，不只是会调包',
          '基础语法、面向对象、异常处理、文件与网络操作'
        ]
      },
      {
        area: '计算机基础',
        items: [
          'HTTP 协议、状态码、请求方法、鉴权方式',
          '数据库基础：增删改查、多表关联、索引概念',
          'Linux 常用命令与日志查看',
          '网络基础：TCP、DNS、代理与抓包'
        ]
      },
      {
        area: '测试理论',
        items: [
          '测试用例设计方法：等价类、边界值、判定表、场景法、错误推测',
          '测试类型：功能、接口、性能、兼容、安全、回归',
          '缺陷管理：生命周期、严重等级、复现步骤书写',
          '测试左移与右移、分层测试策略（单元/接口/UI）'
        ]
      },
      {
        area: '框架与工具',
        items: [
          '接口自动化：requests + pytest + allure 报告',
          'UI 自动化：selenium / playwright、元素定位、等待策略',
          '性能测试：JMeter / Locust 基本使用与指标解读',
          '持续集成：Jenkins / GitLab CI 跑测试流水线',
          '抓包与调试：Charles / Fiddler / 浏览器 DevTools'
        ]
      },
      {
        area: '项目经验',
        items: [
          '一个可运行的自动化测试项目（含用例管理、报告、断言）',
          '能说清框架分层设计与维护成本考量',
          '有接口测试或性能测试的实战数据'
        ]
      },
      {
        area: '算法与数据结构',
        items: [
          '中等难度以下为主：数组、字符串、哈希、简单递归',
          '重点是代码正确性与边界处理，而非炫技',
          '部分公司只考简单-中等难度'
        ]
      },
      {
        area: '八股与面经',
        items: ['测试用例设计题（给一个功能现场设计用例）是必考', '接口测试细节：鉴权、幂等、超时、重试', '缺陷定位思路']
      },
      {
        area: '工程能力',
        items: ['Git', 'CI/CD 流水线', '日志分析', '编写清晰的测试报告']
      }
    ],
    stages: [
      {
        key: 'foundation',
        name: '基础补齐',
        weeks: '第 1–4 周',
        goal: '测试理论 + Python 基础 + HTTP/数据库基础到位',
        weeklyTasks: ['测试用例设计方法每周 2 种并各写 10 条用例', 'Python 基础专题', 'HTTP 与 SQL 基础练习'],
        deliverable: '一份 50 条用例的测试用例集（针对一个真实网站）'
      },
      {
        key: 'drill',
        name: '专项刷题',
        weeks: '第 5–9 周',
        goal: '自动化框架能独立搭起来；算法保持简单-中等水平',
        weeklyTasks: ['pytest 接口自动化实战', 'selenium/playwright 基础', '算法每周 8 道'],
        deliverable: '一个能跑通的接口自动化项目（含报告）'
      },
      {
        key: 'project',
        name: '项目实战',
        weeks: '第 10–13 周',
        goal: '做出一个完整的分层自动化测试框架并接入 CI',
        weeklyTasks: ['框架分层重构（数据驱动 / 断言封装 / 报告）', '接入 CI 流水线', '补性能测试一份'],
        deliverable: '自动化框架代码 + CI 截图 + 测试报告'
      },
      {
        key: 'sprint',
        name: '面试冲刺',
        weeks: '第 14–16 周',
        goal: '现场设计用例不慌，能讲清框架设计取舍',
        weeklyTasks: ['每天 1 个功能设计用例练习', '缺陷定位场景题练习', '模拟面试 3 次'],
        deliverable: '10 个常见功能的用例设计模板'
      }
    ],
    questionList: [
      {
        topic: '测试理论与用例设计',
        category: 'DevOps 运维面试题',
        count: 35,
        difficulty: D(15, 15, 5),
        hot: '等价类与边界值、判定表、场景法、登录/支付/搜索框怎么设计用例、缺陷等级划分',
        handWrite: '现场给一个功能（如登录、购物车）写出 20 条用例'
      },
      {
        topic: '接口测试',
        category: '计算机网络面试题',
        count: 30,
        difficulty: D(12, 14, 4),
        hot: 'HTTP 方法与状态码、鉴权（token/cookie）、幂等性、超时重试、接口依赖处理',
        handWrite: '手写 requests 封装、手写参数化测试'
      },
      {
        topic: 'Python 与自动化',
        category: '算法和数据结构面试题',
        count: 30,
        difficulty: D(12, 13, 5),
        hot: 'pytest fixture 作用域、参数化、selenium 元素定位与等待、PO 模式',
        handWrite: '手写一个 pytest 测试类、手写等待重试装饰器'
      },
      {
        topic: '数据库与 SQL',
        category: 'MySQL 面试题',
        count: 25,
        difficulty: D(10, 11, 4),
        hot: '多表关联查询、聚合、索引对查询的影响、造测试数据',
        handWrite: '手写多表查询、手写数据校验 SQL'
      },
      {
        topic: 'Linux 与网络排查',
        category: '操作系统面试题',
        count: 20,
        difficulty: D(8, 9, 3),
        hot: '日志查看与过滤、进程与端口排查、DNS 与抓包、环境变量',
        handWrite: '手写常用排查命令组合'
      },
      {
        topic: '算法与数据结构',
        category: '数据结构面试题',
        count: 50,
        difficulty: D(22, 24, 4),
        hot: '数组、字符串、哈希、栈队列、简单递归与双指针',
        handWrite: '手写字符串处理、手写数组去重与统计'
      },
      {
        topic: '性能测试基础',
        category: 'DevOps 运维面试题',
        count: 15,
        difficulty: D(5, 7, 3),
        hot: '并发与压测指标（TPS/RT/错误率）、JMeter 元件、瓶颈定位思路',
        handWrite: '手写一个压测脚本配置'
      }
    ],
    portfolio: {
      projects: [
        '一个分层的接口自动化框架（用例层 / 数据层 / 断言层 / 报告）',
        '一个 UI 自动化小项目 + 一次性能测试报告'
      ],
      description: [
        '重点写「框架设计」而不是「写了多少用例」：分层怎么划、数据怎么驱动、断言怎么封装',
        '写清维护成本考量：为什么用 YAML 存用例、怎么处理接口依赖',
        '有 CI 截图和测试报告链接最有说服力'
      ],
      highlights: [
        '一次通过自动化发现的真实缺陷（现象 → 定位 → 提缺陷 → 验证修复）',
        '框架从「一堆脚本」重构为「分层结构」的过程与收益',
        '一次性能测试：压测方案、发现的瓶颈、给出的优化建议'
      ],
      followUps: [
        '你的框架怎么处理接口之间的依赖？',
        '用例执行不稳定（flaky）怎么治理？',
        '为什么选 pytest 不选 unittest？',
        'UI 自动化和接口自动化你怎么分工？',
        '如果开发说「这是预期行为」，你怎么推进？'
      ],
      checklist: [
        '框架能一条命令跑完并生成报告',
        '能讲清每一层的职责边界',
        '有用例失败截图与日志留存机制',
        '已接入 CI（有流水线截图）',
        '能说出至少 3 个自动化踩坑'
      ]
    },
    milestones: [
      { text: '完成 50 条覆盖等价类/边界值的用例设计题', weight: '用例' },
      { text: '独立搭出一个能跑通并出报告的接口自动化框架', weight: '自动化' },
      { text: '算法简单+中等题 46 道完成', weight: '算法' },
      { text: '能现场给任意功能设计 20 条用例不重不漏', weight: '面试' }
    ]
  },

  {
    id: 'devops',
    name: '运维 / DevOps',
    shortName: '运维',
    tagline: 'Linux 底子 + 容器编排 + 自动化流水线',
    months: '3–5 个月',
    weeklyHours: '每周 13–17 小时',
    targetRoles: ['运维开发实习生', 'DevOps 工程师（初级）', 'SRE 实习生', '云原生方向实习生'],
    scenario:
      '运维/DevOps 面试非常「实」：Linux 命令、网络排查、脚本能力会当场考，' +
      '容器与 CI/CD 是当前 JD 的硬要求。' +
      '优势是岗位缺口相对大、对算法要求低；劣势是要求你有真实的动手环境（自己搭一套就有得聊）。',
    jd: [
      {
        area: '编程语言',
        items: [
          'Shell 脚本必须熟练：变量、条件、循环、管道、文本处理（awk/sed/grep）',
          'Python 用于自动化运维脚本与平台开发',
          'Go 在云原生工具链中越来越常见（能读即可，加分项）'
        ]
      },
      {
        area: '计算机基础',
        items: [
          'Linux 系统管理：进程、内存、磁盘、网络、权限、systemd',
          '网络：TCP/IP、DNS、负载均衡、代理、抓包分析',
          '操作系统原理：文件系统、IO 模型、内存管理'
        ]
      },
      {
        area: '容器与编排',
        items: [
          'Docker：镜像分层、Dockerfile 编写、网络模式、数据卷',
          'Kubernetes：Pod/Deployment/Service/Ingress、资源限制、滚动更新、探针',
          'Helm 基本使用（加分）'
        ]
      },
      {
        area: 'CI/CD 与工具链',
        items: [
          'Jenkins / GitLab CI 流水线编写',
          'Git 工作流与分支策略',
          '配置管理：Ansible 基本使用',
          'Nginx 反向代理与负载均衡配置'
        ]
      },
      {
        area: '监控与可观测',
        items: [
          'Prometheus + Grafana 指标体系与告警规则',
          '日志收集（ELK / Loki）',
          '链路追踪基本概念'
        ]
      },
      {
        area: '项目经验',
        items: [
          '一套自己搭起来的服务（含容器化、反代、监控、CI）',
          '能说清一次线上故障的排查过程',
          '有自动化脚本沉淀'
        ]
      },
      {
        area: '算法与数据结构',
        items: [
          '要求最低的方向之一：简单-中等为主',
          '更常考的是脚本编写与命令组合',
          '字符串处理、数组操作、简单递归足够'
        ]
      },
      {
        area: '八股与面经',
        items: ['Linux 命令与排查思路是核心', '容器与 K8s 概念必问', '故障场景题（服务 502 怎么查）']
      }
    ],
    stages: [
      {
        key: 'foundation',
        name: '基础补齐',
        weeks: '第 1–5 周',
        goal: 'Linux 与网络排查形成条件反射，Shell 能写实用脚本',
        weeklyTasks: ['Linux 命令专题每周 2 组', '网络排查场景每周 2 个', 'Shell 脚本每周 2 个实用小工具'],
        deliverable: '一个自己的运维脚本仓库（10 个脚本）'
      },
      {
        key: 'drill',
        name: '专项刷题',
        weeks: '第 6–10 周',
        goal: 'Docker / K8s / CI/CD 三块能独立配置起来',
        weeklyTasks: ['Docker 容器化实战', 'K8s 本地集群（minikube/kind）练习', 'Jenkins 流水线搭建'],
        deliverable: '一套容器化 + 流水线的 demo 环境'
      },
      {
        key: 'project',
        name: '项目实战',
        weeks: '第 11–15 周',
        goal: '完整搭一套带监控告警的服务并演练故障排查',
        weeklyTasks: ['Prometheus + Grafana 接入', 'Nginx 反代与证书配置', '模拟 3 次故障并记录排查过程'],
        deliverable: '架构图 + 监控看板截图 + 3 份故障复盘'
      },
      {
        key: 'sprint',
        name: '面试冲刺',
        weeks: '第 16–18 周',
        goal: '故障排查场景题能条理清晰地答，命令不卡壳',
        weeklyTasks: ['每天 5 个排查场景口述', 'K8s 概念二轮', '模拟面试 3 次'],
        deliverable: '一份 30 个故障场景的排查手册'
      }
    ],
    questionList: [
      {
        topic: 'Linux 系统管理',
        category: '操作系统面试题',
        count: 40,
        difficulty: D(15, 18, 7),
        hot: '进程查看与杀进程、内存与 swap、磁盘与 inode、软硬链接、权限与 umask、systemd',
        handWrite: '手写日志分析脚本、手写批量操作脚本'
      },
      {
        topic: '网络与排查',
        category: '计算机网络面试题',
        count: 35,
        difficulty: D(12, 16, 7),
        hot: 'TCP 状态与连接问题、DNS 解析、502/504 区别、抓包分析、负载均衡原理',
        handWrite: '手写端口连通性检测脚本'
      },
      {
        topic: '容器与 Kubernetes',
        category: 'DevOps 运维面试题',
        count: 40,
        difficulty: D(10, 20, 10),
        hot: '镜像分层与优化、Dockerfile 最佳实践、Pod 生命周期、Service 类型、探针、滚动更新',
        handWrite: '手写多阶段构建 Dockerfile、手写 K8s Deployment + Service YAML'
      },
      {
        topic: 'CI/CD 与自动化',
        category: 'DevOps 运维面试题',
        count: 30,
        difficulty: D(10, 14, 6),
        hot: '流水线阶段设计、制品管理、灰度发布、Ansible playbook、Git 分支策略',
        handWrite: '手写 Jenkinsfile、手写 Ansible playbook'
      },
      {
        topic: 'Nginx 与中间件',
        category: 'Nginx 面试题',
        count: 20,
        difficulty: D(8, 9, 3),
        hot: '反向代理与负载均衡策略、限流、静态资源缓存、HTTPS 配置、常见 502 原因',
        handWrite: '手写 Nginx 反代与负载均衡配置'
      },
      {
        topic: '监控与可观测',
        category: 'DevOps 运维面试题',
        count: 20,
        difficulty: D(6, 10, 4),
        hot: 'Prometheus 指标类型、告警规则、Grafana 面板、日志采集、SLI/SLO',
        handWrite: '手写 Prometheus 告警规则'
      },
      {
        topic: '算法与数据结构',
        category: '数据结构面试题',
        count: 40,
        difficulty: D(18, 18, 4),
        hot: '字符串处理、数组操作、哈希计数、简单递归',
        handWrite: '手写字符串解析、手写简单排序'
      }
    ],
    portfolio: {
      projects: [
        '一套自建的服务环境：容器化 + Nginx 反代 + Prometheus 监控 + CI 流水线',
        '一个运维自动化脚本仓库（备份、巡检、日志清理等）'
      ],
      description: [
        '画一张完整架构图，从请求入口到后端服务到数据层，标出每一层用的工具',
        '写清自动化程度：哪些原来是手工的，现在用脚本/流水线替代，节省了多少时间',
        '故障复盘用 STAR：现象、影响面、排查过程、根因、改进措施'
      ],
      highlights: [
        '一次真实故障的排查：从告警到定位到恢复的完整链路',
        '镜像体积或部署时间优化的前后对比数据',
        '监控告警体系的搭建思路：告什么、阈值怎么定、怎么避免告警疲劳'
      ],
      followUps: [
        '服务返回 502，你的排查顺序是什么？',
        'K8s 里 Pod 一直 CrashLoopBackOff 怎么查？',
        '你的流水线为什么这么分阶段？',
        '告警太多怎么治理？',
        '如何做到不停机发布？'
      ],
      checklist: [
        '能画出架构图并解释每个组件的作用',
        '能现场写出常用的排查命令组合',
        '有监控看板截图与告警规则配置',
        '有 CI 流水线截图',
        '能说出 3 次真实故障的排查过程'
      ]
    },
    milestones: [
      { text: 'Linux 与网络排查合计 75 道完成，命令不再卡壳', weight: '基础' },
      { text: '独立完成一套容器化 + 反代 + 监控 + CI 的环境', weight: '项目' },
      { text: '能条理清晰地答完 30 个故障排查场景题', weight: '排查' },
      { text: 'K8s 核心对象 YAML 能默写并讲清作用', weight: '编排' }
    ]
  }
]

/** 页面顶部提示：内容会随招聘市场变化，避免用户当成永久标准 */
export const PATH_DISCLAIMER = '路线依据当前市面实习 / 初级岗位 JD 的常见要求归纳，未引用任何具体公司的招聘信息。招聘要求在持续变化，建议每季度对照最新 JD 复核一次。'
