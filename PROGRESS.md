# PROGRESS · 码上记（CodeInterview）

> 执行模式：开发 Agent 全权执行（站长不参与）：自己建库、自己验收、自己 commit、自己进下一张卡。
> 规范真源：`开发文档/`（README + 01–07）。本文件只记录**执行进度与真实验收输出**，不重复定义规范。

## 检查点进度

| 检查点 | 卡片 | 状态 |
|---|---|---|
| A1 工程基建 | T01 / T02 / T03 | ✅ 通过（health UP、404 走 handler、参数 400、seed 入库） |
| A2 登录鉴权 | T04 | ✅ 通过（注册/登录/刷新保持/登出/改名改密/锁定） |
| A3 题库只读 | T05 / T06 / T07 | ✅ 通过（首页/列表/详情/代码高亮/登录/刷新保持，浏览器实测） |
| B1 学习闭环 | T08 / T09 / T10 / T11 | ✅ 通过（收藏→错题→掌握→统计，浏览器 16/16；code-review 已完成） |
| B2 管理端 | T12 / T13 | ✅ 通过（分类标签/导入/建题发布下架，浏览器 11/11；code-review 已完成） |

## 卡片记录

### T01 · 仓库骨架与前端脚手架 ✅

**改动文件**
- `frontend/`：`package.json`、`package-lock.json`、`vite.config.js`、`eslint.config.js`、`.prettierrc.json`、`.nvmrc`、`index.html`、`.env.development`、`.env.production`、`src/main.js`、`src/App.vue`、`src/router/index.js`（T04 会重写）、`src/assets/styles/base.css`
- 根 `.gitignore`（按 05 §2.4 清单重写）
- `backend/config/application-local.yml.example`（模板，值全空）
- `开发文档/`（8 份文档入库）

**验收（真实输出，2026-11-05）**
- `node -v` → `v24.18.1`；`npm -v` → `11.16.0`
- `npm ci` → `added 276 packages in 15s`
- `npm run lint` → exit 0（`eslint .` 无告警）
- `npm run build` → `✓ 36 modules transformed` / `dist/assets/index-*.js 90.71 kB │ gzip: 35.14 kB` / `✓ built in 158ms`
- `npm ls vue vite --depth=0` → `vite@8.3.5`、`vue@3.5.44`，无 `UNMET`
- `npm run dev` → `http://127.0.0.1:5173` 占位页显示「码上记」；375×667 实测 `scrollWidth=375, clientWidth=375`（无横向滚动）
- `git check-ignore -v backend/config/application-local.yml` → `.gitignore:2:backend/config/application-local.yml` 命中；`git status` 看不到该文件

**偏离登记**：见 `开发文档/02 §10`（manualChunks 函数写法、.gitignore 放行前端 env）。

### T02 · 后端骨架与统一返回 ✅

**改动文件**
- `backend/pom.xml`
- `CodeInterviewApplication.java`
- `common/result/{Result,ResultCode,PageResp}.java`
- `common/exception/{BusinessException,GlobalExceptionHandler}.java`
- `common/config/{JacksonConfig,MybatisPlusConfig,OpenApiConfig}.java`
- `common/handler/MetaObjectHandler.java`
- `health/HealthController.java`
- `resources/{application.yml,application-dev.yml,logback-spring.xml}`

**验收（真实输出，2026-11-05）**
- `mvn -q clean package -DskipTests` → exit 0，产物 `target/codeinterview-backend-1.0.0.jar`
- 无库启动 → `Started CodeInterviewApplication in 2.158 seconds`；`GET /api/v1/health` → HTTP 503 `{"status":"DOWN","db":"DOWN","version":"1.0.0","time":"..."}`
- `GET /api/v1/nope` → HTTP 404 `{"code":40400,"message":"内容不存在或已下架","data":null}`
- 非法 JSON POST → HTTP 400 `{"code":40000,"message":"请求体格式错误"}`；`{}` → 400 `nickname/password` 校验文案
- `git check-ignore -v backend/config/application-local.yml` 命中；`git status` 无 `target/`、`logs/`、`application-local.yml`

**偏离登记**：见 `开发文档/02 §10`（Jackson 只转 Long 包装类型；额外覆盖 3 类异常）。

### T03 · 数据库初始化与 seed、管理员初始化 ✅

**改动文件**
- `db/init.sql`（03 §3 全 11 张表 + §6 seed：5 分类 / 15 标签 / 3 草稿题 / question_tag 6 行）
- `db/grant.sql.example`（建库 + 应用账号授权，密码位留空）
- `docker/docker-compose.yml`（mysql:8.4 备选，3307:3306，挂载 init.sql，healthcheck）
- `backend/.../auth/AdminInitializer.java`（无 role=1 时按 profile 分级建 admin）
- `README.md`（启动/部署/管理员初始化/端口/常见问题）
- `backend/src/main/resources/application.yml`（JDBC `characterEncoding` 修正 + sa-token 配置）

**验收（真实输出，2026-11-05）**
- root 用 `db/init.sql` 建库；应用账号 `codeinterview/123456` 可直接连库
- `show tables` → 11 张（ai_chat_log/category/practice_record/question/question_tag/site_ai_usage/tag/user/user_ai_quota/user_favorite/wrong_record）
- `question=3、category=5、tag=15、question_tag=6`；中文分类正常
- `select id,nickname,role,status,length(password)` → `1 admin 1 1 60`（BCrypt cost 12）
- 后端启动后 `GET /api/v1/health` → HTTP 200 `{"status":"UP","db":"UP",...}`
- `AdminInitializer` 分支：已存在 → skip；未配置 init-password → WARN 且不建号；DB 不通只 WARN 不崩
- `docker compose config --quiet` → exit 0（未启动容器）

**偏离登记**：见 `开发文档/02 §10`（JDBC `characterEncoding=utf8mb4` 文档值非法，实际用 `UTF-8`；T03 登录验收顺延 T04）。

### A1 检查点自检 ✅

- `GET /api/v1/health` → HTTP 200，`db=UP`
- `GET /api/v1/nope` → HTTP 404 `{"code":40400}`
- 非法 JSON / 缺字段 POST → HTTP 400 `{"code":40000}`
- `mvn -q clean package -DskipTests` → exit 0；`npm run lint`、`npm run build` → exit 0

### T04 · 登录鉴权闭环 ✅

**改动文件**
- 后端：`auth/{AuthController,AuthService,LoginAttemptGuard,PasswordPolicy}.java`、`auth/{dto,vo}/`、`user/{UserService,UserMapper,entity/User}.java`、`common/config/SaTokenConfig.java`、`common/interceptor/AuthInterceptor.java`、`common/util/IpUtil.java`
- 前端：`src/api/auth.js`、`src/stores/user.js`、`src/utils/request.js`、`src/router/index.js`、`src/views/{Login,Register,Profile,Forbidden,NotFound}.vue`、`src/components/AppHeader.vue`、`src/layout/DefaultLayout.vue`、`src/App.vue`

**验收（真实输出，2026-11-05，后端 curl + 前端 headless Edge/CDP 实测）**
- 注册 `Tom` → `{"code":0,"data":{"userId":"3"}}`；注册 `tom`（大小写变体重复）→ HTTP 409 + `40900`
- `123456` 通过；`11111111`、`123456789`、密码=昵称 均 40000 拒绝；非法昵称 40000
- 登录 `Tom/123456` → `{token,user:{id:"3",nickname:"Tom",role:0}}`；`/auth/me` 无 `username` 字段
- 无 token `/auth/me` → HTTP 401 + 40100；登出后旧 token → 40100
- 不存在的账号与错误密码 → 同一 `40101`/同文案
- 同 IP+昵称连续 5 次错误后第 6 次 → HTTP 429 + `42903`
- 改昵称：缺密码 40000；密码错 40000 且昵称不变；重名 40900；成功 `Tom→Tom2` 后旧 token 40100、旧昵称登录 40101、新昵称登录成功
- 改密码：原密码错 / 新旧相同 / 弱密码 均 40000；成功后旧 token 40100、新密码登录成功
- 前端：注册→跳登录并提示注册成功；登录→sessionStorage 存 token（localStorage 为空）；刷新后 `fetchMe` 保持登录；新标签页无 sessionStorage → 跳登录带 redirect；游客 `/favorites` → `/login?redirect=/favorites` 登录后回跳；登出清态；伪造 token 401 清态跳登录；40101 卡片内提示（0 toast）；375×667 登录页无横向滚动、抽屉 78% 宽
- `npm run lint`、`npm run build` 绿（`1707 modules`，gzip 39.62 kB）

**偏离登记**：见 `开发文档/02 §10`（`/favorites` 401 与管理端 40301 验收顺延；个人中心「注册时间」无接口来源显示「—」；密码递增阈值前后端统一为 ≥7）。

### T05 · 分类/标签 + 题目列表 ✅

**改动文件**
- 后端：`taxonomy/{CategoryController,CategoryService,CategoryMapper,TagController,TagService,TagMapper}.java`、`taxonomy/vo/{CategoryResp,TagResp}.java`、`question/{QuestionController,QuestionService,QuestionMapper}.java`、`question/dto/QuestionQueryReq.java`、`question/vo/{QuestionBriefResp,TagBriefResp,QuestionTagRow}.java`、`resources/mapper/QuestionMapper.xml`
- 前端：`api/{taxonomy,question}.js`、`composables/usePagination.js`、`components/{QuestionCard,DifficultyTag,PageBar,EmptyState,ErrorState}.vue`、`views/QuestionList.vue`、`router/index.js`（仅 `/question` 组件替换）

**验收（真实输出，2026-11-05）**
- `GET /questions?page=1&pageSize=10` → `{page:1,size:10,total:2,pages:1,list:[...]}`，`id` 均为字符串；列表项无 `answerMd`
- `GET /questions?pageSize=500` → 实际 `size=50`
- `GET /questions?keyword=HashMap` → total=1；`?keyword=%25`（`%`）→ total=0、HTTP 200 不报错
- `?sort=hot` → practice_count 5/2 降序；`?sort=xxx` 回落 latest
- `?categoryId=1&difficulty=2` → 命中 2 条（临时数据）
- `GET /categories` → 5 条，`questionCount` 只算发布态（Java=2，其余 0）；`GET /tags` → 15 条含 0 题标签
- 前端（headless Edge/CDP）：首屏 2 卡（标题/难度/分类/标签/浏览/已练）；URL 还原筛选；筛选写 URL 且 `router.replace` 不增历史栈；300ms 防抖实测；刷新保持；骨架屏 6 条；ErrorState+重试；失败时保留旧数据+顶部细条；分页（CDP mock 25 条/3 页）与 `status=2` 置灰；375×667 无横向滚动、搜索/排序/筛选按钮高 44、底部筛选抽屉 + chips 可清除
- `npm run lint`、`npm run build` 绿（1743 modules，列表 chunk gzip 22.79 kB）

**说明**：`question/{Controller,Service,Mapper}.java` 与 `QuestionMapper.xml` 是 T05/T06 共用文件，T05 提交时已包含 T06 的只读详情实现（同文件无法按卡拆分，已在 02 §10 登记）。

### T06 · 题目详情 + Markdown 渲染链 ✅（前端）

**改动文件（本卡提交只含前端；后端 question 三件套随 T05 提交）**
- `frontend/src/views/QuestionDetail.vue`、`components/MarkdownView.vue`、`composables/useMarkdown.js`、`assets/styles/markdown.css`、`api/question.js`（详情）、`router/index.js`（仅 `/question/:id` 一行）

**验收（真实输出，2026-11-05，headless Edge/CDP + 真实接口临时数据）**
- 详情：面包屑「题库 / Java / 题目」、H1、meta（难度/分类/浏览/已练）、标签；答案默认折叠，点击「展开参考答案 ▾」展开，刷新回到折叠
- Shiki：题干 Java 围栏代码块渲染出 `.shiki` 与 22 个着色 span；未注册语言/异常退化 `<pre><code>`（模块级验证 `md-code-fallback`）
- XSS：临时在 q1 题干加入 `<script>alert("xss")</script>`、`<img src=x onerror=...>` → DOM 中 `script`=0、`[onerror]`=0，innerHTML 无 `<script`/`onerror`
- 外链 hook：输出含 `target="_blank" rel="noopener noreferrer"`；`javascript:` 被清洗
- 浏览计数：连续刷新详情 3 次显示 10→11→12（后端异步 +1 生效）
- 上下题：q1 上一题禁用、下一题 → q2；q2 上一题 → q1、下一题禁用；草稿 `/question/3` → 40400 原地渲染 `NotFound`（URL 不变、不跳路由）
- 375×667：无横向滚动；H1 24px；展开/上下题按钮高 ≥44px
- 首屏 gzip ≈87KB；Shiki 919KB（gzip 264KB）为详情页异步 chunk，不进首屏
- `npm run lint`、`npm run build` 绿（1839 modules）

**说明**：联调用的临时数据（q1 题干代码块/XSS、q1/q2 发布、q2 分类临时改 1、practice_record 3 条、view_count）已全部还原为 seed 原状。

### T07 · 首页聚合 ✅

**已完成（后端，随本卡后端 commit）**
- 改动文件：`home/{HomeController,HomeService,HomeMapper}.java`、`home/vo/HomeResp.java`（06 清单外新增 HomeMapper，已登记 02 §10）
- 实现：`GET /api/v1/home`；categories（只返 status=1，questionCount 只算发布态）、latest ≤6（published_at DESC,id DESC）、hot ≤6（practice_count DESC,id DESC）、continueLearning 仅登录返回最近 3 条（每题取最新自评档位，未登录 null）；三块 SQL 分别 try/catch，任一块失败只该字段为空并记 ERROR，不整体 500；标签 IN 批量补全无 N+1

**已完成（前端）**
- 改动文件：`frontend/src/views/Home.vue`、`frontend/src/api/home.js`、`router/index.js`（仅 `/` 一行）
- 结构：Hero（标题/副标题/一句话/两个按钮 + 装饰插画）→ 分类 5 卡自适应 → 最新/热门双栏 → 登录后「继续刷」3 卡；「查看全部」补充入口
- 状态：Hero+双栏骨架；空库 EmptyState「题库建设中」+ 管理员「去录题」；`latest`/`hot`/`continueLearning` 分块 ErrorState，整页请求失败有整页 ErrorState + 重试；游客不显示继续刷
- 视觉：全部走 04 B6 token（主色 #2563eb / 圆角 12·8·4 / 字号 H1 28·36 / 断点 320·768·1024·1280），无新依赖/字体/硬编码色值；375×667 无横向滚动、分类 2 列、双栏单列、继续刷横滑

**验收（真实输出，2026-11-05，headless Edge/CDP + Lead 浏览器实测）**
- 游客 `/home` → HTTP 200，`continueLearning=null`，latest/hot 各 3 条（临时 3 题），分类题数 1/1/1/0/0
- 登录 admin 后 `/home` → `continueLearning` 3 条真实数据：Redis「不会」17:51 / MySQL「模糊」17:41 / HashMap「掌握」17:31，按时间倒序
- 分块降级：CDP mock 单块 null → 只该块 ErrorState，其余块正常；整页失败 → 整页 ErrorState + 重试恢复
- 空库（mock）：EmptyState「题库建设中」，游客无按钮，管理员「去录题」→ `/admin/questions/edit`
- 375×667：Hero 按钮上下整宽、分类 2 列、双栏单列、继续刷横滑、无横向滚动；`npm run lint`/`npm run build` 绿（1842 modules，Home chunk gzip 6.06 kB）

### A3 检查点（浏览器全链路，Lead 实测）✅

- 打开 `http://localhost:5173` → 首页：Hero + 5 分类（Java/MySQL/Redis 各 1 题）+ 最新/热门各 3 条 + 页脚；未登录显示「登录」
- 点 Java 分类 → `/question?categoryId=1`：筛选栏 Java 选中、搜索/排序正常、「共 1 题」、卡片可见
- 点卡片 → `/question/1`：面包屑/H1/meta/标签/题干完整；点「展开参考答案 ▾」→ 答案展开（按钮变「收起 ▴」）；题干 Java 代码块 **Shiki 着色正常**（截图留档）
- 点「登录」→ `/login` 整屏卡片无 Header/Footer；admin/123456 登录成功 → 回首页；Header 显示账号菜单（个人中心/刷题记录/**管理端**/退出登录），「继续刷」3 卡出现
- `Ctrl+R` 刷新 → 仍为登录态（账号菜单 + 继续刷保持），验证 sessionStorage + fetchMe
- 数据为临时发布 3 题 + admin 3 条自评记录，验证后已还原 seed 原状

### 代码审查与修复（A2/A3）

- **Spec 轴审查**（T04–T07）发现 3 个真实缺陷，已修复并用真实请求复验：
  1. `/home` 分块失败返回 `[]` 而前端按 `null` 判定 ErrorState → 后端改为**失败返回 null、成功无数据返回 []**；实测临时 `RENAME TABLE question` 后 `/home` HTTP 200 且四块为 null（前端可显示分块 ErrorState），恢复后正常。
  2. `/questions?page=99`（total>0 但页越界）时 `attachTags` 生成 `IN ()` 导致 500 → 空列表保护；实测 HTTP 200 + `list:[]`。
  3. 登录失败锁定键未对昵称大小写归一，`Tom/tom` 交替可绕过 → 键改为小写归一；实测 `LockMe/lockme` 交替 5 次后第 6 次 42903。
- **Standards 轴审查**（T04–T07）剩余项已修复：
  1. Markdown 兜底 `renderPlainText` 现在也经 `DOMPurify.sanitize`（02 §4.4「始终执行」）。
  2. `User` 增加 `ROLE_*` / `STATUS_*` / `DELETED` 常量，`UserService`、`AdminInitializer`、`AuthService`、`AuthInterceptor`、`SaTokenConfig` 去除 role/status/deleted 魔法值。
  3. 注册关闭 40300：`/auth/register` 请求不再被全局护栏跳 `/403`，由注册页原地提示（04 B4.5）。
  4. 题库筛选单选项触控高度 40→44px（04 B7）；`rgba()` 阴影/软背景改为 `rgba(var(--*-rgb), α)` token（04 B6）。
  5. 首页「继续刷」窗口函数 SQL 偏离 03 §8.8 已登记 02 §10。
- 仍未处理（判断项、接受现状）：`attachTags` 在 `QuestionService`/`HomeService` 各有一份（重复但语义不同，重构收益低）；密码校验前端/后端各一份（契约要求两端一致，属预期重复）。
- 另补登记 02 §10：`user/UserController` 未建（无 `/users` 接口）、昵称查重含已删除行（按 03 §4.4 昵称永不复用）、个人中心「注册时间」无接口来源、首页分块失败语义。
- Standards 轴审查（依赖/命名/安全/前端红线）在 A1 已跑并修复提交自洽性问题；A2/A3 的 Standards 复跑建议在会话 B 开工前补一次（本会话末未阻塞交付）。

---

## 【交接 · 会话 A】

- **已完成**：T01–T07（A1=T01-T03、A2=T04、A3=T05-T07 全部检查点通过）
- **提交记录**（`git log --oneline -8`）：
  ```
  <本卡前端 commit> feat(T07): 首页聚合（前端）
  b0c06a7 feat(T07): 首页聚合（后端，前端待续）
  cef1e8b feat(T06): 题目详情 + Markdown 渲染链（前端）
  2f2fa11 feat(T05): 分类/标签 + 题目列表（筛选/排序/分页）
  c3cf51c feat(T04): 登录鉴权闭环（昵称即登录名）
  5c2130c feat(T03): 数据库初始化与 seed、管理员初始化
  70f5b5b feat(T02): 后端骨架与统一返回
  9d23bc7 feat(T01): 前端脚手架与工程规范
  ```
- **文档偏离**：见 `开发文档/02 §10`（JDBC characterEncoding、Jackson 只转 Long、密码递增阈值、验收顺序顺延、共用文件提交粒度、T05/T06/T07 辅助类等 12 条）
- **已知问题**：
  1. T07 前端「看看我的错题」指向 `/wrong-book`（游客经守卫跳登录），与 07 提示词里「Hero 按钮跳 /question」不同；按 04 B4.1 按钮语义实现，待站长确认。
  2. 个人中心「注册时间」无接口来源，显示「—」。
  3. `/favorites` 无 token 401、管理端 40301 分别顺延到 T08/T12 验证。
  4. 首页「继续刷」在无自评记录时不显示该区块（04 未要求空态）。
  5. 单测：本会话尚未写（06 规定单测随 T14/T15/T18 落地，T20 全量回归）。
- **测试**：`mvn -q -DskipTests package` 通过；`npm run lint`、`npm run build` 绿；`mvn -q test` 当前为 0 tests（符合本会话范围）。
- **PROGRESS.md**：已更新到 T07（本文件）。
- **恢复起点（会话 B）**：T08 收藏；数据从 seed 3 草稿题 + admin 开始，按 07「会话 B」提示词执行 T08–T13。

---

## 【会话 B】任务卡记录（T08–T13）

### T08 · 收藏 ✅

**改动文件**
- 后端（新增）：`favorite/{FavoriteController,FavoriteService,FavoriteMapper}.java`、`favorite/vo/{FavoriteStateResp,FavoriteQuestionResp}.java`
- 前端：`src/views/Favorites.vue`（新增）、`src/views/QuestionDetail.vue`（收藏按钮 + 乐观更新）、`src/api/question.js`、`src/router/index.js`（`/favorites` 换真实组件）

**验收（真实输出，2026-11-05）**
- `POST /questions/1/favorite` ×3 → 三次均 `{"favorited":true}`；`SELECT COUNT(*) FROM user_favorite` = 1（q1），列表 total=3（q1/q2/q3）无重复行
- 列表倒序：`ids=3,2,1`（按 `favorited_at DESC`）；首项字段 `id,title,difficulty,categoryId,categoryName,tags,viewCount,practiceCount,status,publishedAt,favoritedAt` 齐全
- `DELETE /questions/1/favorite` → `{"favorited":false}`；再收藏成功，列表 total 仍 3（无逻辑删除残留）
- 无 token → HTTP 401 `40100`；`/questions/999/favorite` → HTTP 404 `40400`；`?page=99` → HTTP 200 `list:[]`；`?pageSize=500` → `size=50`
- 下架口径：临时 `status=2` → 收藏列表仍返回该题且 `status=2`，详情 `/questions/1` → HTTP 404 `40400`，再次收藏 → 404；恢复 `status=1` 后列表 `status=1`
- 浏览器（headless Edge/CDP，真实接口）：游客 `/favorites` → `/login?redirect=/favorites`；登录回跳；标题「我的收藏」、计数「共 3 道」、3 卡；取消收藏 → 2 卡且计数减 1；详情「☆ 收藏」→ 点击「★ 已收藏」→ 刷新仍「★ 已收藏」→ 再点回「☆ 收藏」；收藏页剩 2 条
- 375×667：收藏页 `scrollWidth=375, clientWidth=375`、取消收藏按钮高 44px 可点；详情页无横向滚动、收藏按钮高 44px；游客点收藏弹「登录后才能收藏，去登录？」
- `mvn -q clean package -DskipTests` exit 0；`npm run lint` / `npm run build` exit 0（Favorites chunk gzip 1.66 kB）

**偏离登记**：见 `开发文档/02 §10`（T08 改动文件未列 `QuestionDetail.vue` 与辅助 VO，但「详情页收藏按钮」与 Result 出参类型属本卡职责）。

### T09 · 三档自评 + 刷题记录 ✅

**改动文件**
- 后端（新增）：`practice/{AssessmentController,AssessmentService,PracticeRecordController,PracticeRecordService,PracticeRecordMapper,WrongRecordMapper}.java`、`practice/entity/PracticeRecord.java`、`practice/dto/AssessmentReq.java`、`practice/vo/{AssessmentResp,WrongBookState,PracticeRecordResp}.java`
- 后端（共享文件修改）：`question/QuestionMapper.java`（新增 `increasePracticeCount` 原子自增）
- 前端：`src/components/AssessmentBar.vue`（新增）、`src/views/QuestionDetail.vue`（自评区 + 2s 防抖 + source）、`src/views/Records.vue`（新增，记录列表部分）、`src/api/record.js`（新增）、`src/router/index.js`（`/records` 换真实组件）

**验收（真实输出，2026-11-05，真实 DB + curl + headless Edge/CDP）**
- 状态机（`01 §8.3` 五条转移，全部实测）：
  - 自评 3 → `practice_record`=1、`wrong_record`=(wrong_count=1, resolved=0, last_assess=3, mastered_at NULL)、`question.practice_count` +1
  - 再评 2 → wrong_count=2、last_assess=2、todayCount=2
  - 再评 1 → resolved=1、last_assess=1、mastered_at 非空、wrong_count 保持 2
  - 再评 3 → resolved=0、mastered_at=NULL、last_assess=3、wrong_count=3；q1 practice_count 累计 +4
  - 从未进过错题本的题自评 1 → `wrongBook=null`，`wrong_record` 行数 0（不建行）
  - 手动移除分支由 T10 实现
- `source`：`{"selfAssess":2,"source":3}` → 记录 `source=3`；`/practice-records?selfAssess=1` 筛选 total=1；列表首行 `selfAssess/source/createdAt` 正确
- 事务：一次 upsert SQL 报错（`Column 'wrong_count' ambiguous`）时，同一事务内已插入的 `practice_record` 与 `practice_count` 全部回滚为 0（**真实失败验证了 02 §5.8 的原子性**）；修复 SQL 后重跑通过
- 校验：`selfAssess=4` → HTTP 400 `40000`（文案定位字段）；`{}` → HTTP 400 `40000`；`/questions/999/assessment` → 404 `40400`；草稿题（临时 `status=0`）→ 404 `40400`；无 token → 401 `40100`
- `todayCount` 与 `SELECT COUNT(*) ... created_at >= CURDATE()` 一致（8 == 8）
- 浏览器：详情页 3 个自评按钮（掌握/模糊/不会）；初始高亮与 `GET /questions/1` 的 `myAssessment` 一致；点击后按钮全部禁用（2s 防抖实测）、`今日已刷 N 题` 出现、2s 后恢复可点；「不会」轻提示「已记录：不会，已加入错题本」；`/records` 表格 10 行、筛选「掌握」生效、来源列显示「详情页/错题本」；375×667 记录页转卡片、无横向滚动 `sw=375/cw=375`、自评按钮高 44px；游客点自评弹「登录后才能自评，去登录？」
- `mvn -q clean package -DskipTests` exit 0；`npm run lint` / `npm run build` exit 0

**偏离登记**：见 `开发文档/02 §10`（新增 `/practice-records` 控制器与辅助类、T09 提前建 `WrongRecordMapper`、详情页今日数初始无接口来源）。

### T10 · 错题本 ✅

**改动文件**
- 后端（新增）：`practice/{WrongBookController,WrongBookService}.java`、`practice/vo/{WrongBookItemResp,WrongResolvedResp}.java`
- 后端（扩展）：`practice/WrongRecordMapper.java`（列表/计数/标记掌握/彻底移除/跨用户判定）
- 前端：`src/views/WrongBook.vue`（新增）、`src/api/record.js`、`src/router/index.js`（`/wrong-book` 换真实组件）

**验收（真实输出，2026-11-05，真实 DB + curl + headless Edge/CDP）**
- 列表：`GET /wrong-book?resolved=0` → `total=1`，字段齐全且同名同义 `questionId,title,difficulty,categoryName,lastAssess,wrongCount,lastWrongAt,resolved,status`；`status=1`
- 标记掌握：`PUT /wrong-book/1/resolved` → `{"resolved":1}`；DB `resolved=1, last_assess=1, mastered_at 非空, wrong_count 保持 10`（三列同写、错次不变）；未掌握 total=0、已掌握 total=1；再次 PUT 幂等返回 `{"resolved":1}`
- 已掌握后再自评「模糊」→ 响应 `resolved=0`，DB `resolved=0, mastered_at=NULL, last_assess=2, wrong_count 10→11`（回到未掌握，`01 §8.3` 第 4 条转移）
- 彻底移除：`DELETE /wrong-book/1` → 200，DB 行数 0；再删一次仍 200；无记录 PUT → 404 `40400`；`DELETE /wrong-book/999` → 200
- 跨用户：注册 `t10user`（id=5）对 q2 自评「模糊」产生其错题行；admin `PUT /wrong-book/2/resolved` → HTTP 403 `40300`（不区分不存在/不属于你）；admin `DELETE /wrong-book/2` → 200 且 t10user 行仍在（04 A3.4 #18 不存在也成功，不泄漏归属）
- 已下架/已删除：`q3` 置 `status=2` → 列表仍返回且 `status=2`；`q3` 置 `deleted=1` → 列表不返回，但 `wrong_record` 行仍保留；均按 03 §8.4 口径
- 分页：`pageSize=500` → 实际 `size=50`；`page=99` → `list:[]`
- 浏览器：未掌握 tab 表格行含「去复习/标记掌握/彻底移除」；标记掌握后该行消失；已掌握 tab 的**自评列显示「掌握」**（而非模糊，F05 验收点）；彻底移除弹「移除后该题的历史错题记录将不可恢复」→ 确认后空态「还没有已掌握的错题」；「去复习」跳 `/question/2?from=wrong` 且随后自评记录 `source=3`；375×667 卡片化、无横向滚动 `375/375`、操作按钮高 44px
- 已下架 UI：`q2` 临时 `status=2` 时桌面「去复习」按钮 disabled、移动端卡片显示「题目已下架，仅供查看」且按钮 disabled；恢复 `status=1`
- `mvn -q clean package -DskipTests` exit 0；`npm run lint` / `npm run build` exit 0

**偏离登记**：见 `开发文档/02 §10`（辅助 VO 清单、跨用户 DELETE 口径按 04 #18、markResolved 用影响行数 + 存在性判定区分 40400/40300）。

### T11 · 统计与图表 ✅

**改动文件**
- 后端（新增）：`stats/{StatsController,StatsService,StatsMapper}.java`、`resources/mapper/StatsMapper.xml`、`stats/vo/{StatsOverviewResp,TrendPointResp,CategoryStatResp,StatsSummaryRow,TrendRow}.java`
- 前端（新增）：`src/components/BaseChart.vue`、`src/api/stats.js`
- 前端（修改）：`src/views/Records.vue`（5 指标卡 + 折线/饼图，保留 T09 记录列表）、`src/assets/styles/base.css`（图表 5 色 token）

**验收（真实输出，2026-11-05，真实 DB + curl + headless Edge/CDP）**
- `GET /stats/overview` → `{"todayCount":20,"weekCount":20,"masteredRate":0.25,"streak":1,"totalQuestionCount":3,"totalRecordCount":20}`；与手写 SQL 逐项一致：today 20 / week 20 / total 20 / mastered 5 / distinct 3；`totalQuestionCount(3) ≤ totalRecordCount(20)`
- 两个总量口径分开：`totalQuestionCount`=去重题数（3），`totalRecordCount`=条数（20），分类合计只与后者对得上
- `GET /stats/trend?days=7` → 7 条连续日期、首尾相差 6 天、末日=今日；`days=30` → 30 条；`days=15` → 回落 30 条
- `GET /stats/category` → 3 类（Java count=15 rate=0.2667 / MySQL count=3 rate=0.3333 / Redis count=2 rate=0.0），`count` 合计 20 == `totalRecordCount`
- 全新用户（t11user）：`masteredRate=null`、全 0、`streak=0`、trend 全 0、category `[]`（前端显示「—」与「暂无数据」，不渲染空坐标系）
- 连续天数：临时给 t11user 插入「昨天 + 前天」记录 → `streak=2`（今日无记录从昨日算起）；再加今日 → `streak=3`；顺带核对 `totalQuestionCount=2 / totalRecordCount=3`
- 浏览器：5 卡「今日自评 / 近 7 天 / 掌握率 / 连续天数 / 累计题数」= `20 | 20 | 25% | 1 天 | 3`，累计题数卡标注「共 20 次自评」；两张 ECharts canvas 渲染（每日自评趋势 / 分类掌握分布）；切 30 天只重拉 trend（trend 请求 1→2、overview 仍 1 次）；375×667 指标卡 2 列且第 5 张整行（last=343 vs first=166）、图表上下堆叠同宽、无横向滚动 `375/375`、图表高 240px；新用户两图均显示「暂无数据」且 canvas=0
- `mvn -q clean package -DskipTests` exit 0；`npm run lint` / `npm run build` exit 0

**偏离登记**：见 `开发文档/02 §10`（新增 `StatsMapper` 与辅助 VO、指标卡共用单接口的失败表现、图表色 token 落在 base.css）。

### B1 检查点（T08–T11 学习闭环）✅

**浏览器全链路（headless Edge/CDP，真实接口与真实 DB）16/16 通过**
- 登录 → 详情页「今日已刷 22 题」首屏可见 → 收藏 q1（★ 已收藏）→ 我的收藏 3 条可见 → 自评「模糊」（提示「已加入错题本」）→ 错题本出现 q1 且档位=模糊 → 去复习（`?from=wrong`）→ 改评「掌握」→ 未掌握 tab 消失、已掌握 tab 出现且档位=掌握 → 刷题记录页 5 指标卡（24 | 24 | 29% | 1 天 | 3）+ 两张 ECharts + 记录列表 10 行
- 375×667：收藏 / 错题本 / 刷题记录均无横向滚动（`scrollWidth=375, clientWidth=375`）
- 自评按钮：点非当前档位时该按钮立即 loading、三个按钮全部禁用、2s 后恢复；「今日已刷」随自评更新（25→27）

**代码审查（code-review，fixed point `8465c78` = T07 提交）**
- **Standards 轴** 发现 7 项，已修 5 项：分页归一化散落 5 处（新增 `common/util/PageUtil` 统一，Controller 不再归一化）、`markResolved` 与 `markMastered` 两条相同 UPDATE（删除前者复用后者）、图表轴色写死十六进制（改读 `--color-border`/`--color-text-secondary`）、切页用 `window.scrollTo`（改 `scrollIntoView`）、自评 loading 只覆盖当前档位（改乐观切档位）。2 项按判断保留并登记 02 §10：`attachTags` 三份（单题/批量语义不同）、新增 DTO/VO 未加 `@Schema`（全仓既有类一致，springdoc 默认关闭）。
- **Spec 轴** 确认 2 项并修复：详情页「今日已刷」首屏缺失（改调 `GET /stats/overview`，不新增接口）、错题本标记掌握遇 40400 未刷新列表（已补 `loadList`）。1 项属 T16（「问助手」按钮），不计本段。scope creep 2 项已处理：删除未使用的 `getNeighbors` 封装；`BaseChart.ariaLabel` 作为无障碍可选 prop 保留并登记。
- 逐项裁决与偏离记录见 `开发文档/02 §10`（2026-11-05 review 行）。

**回归**：T09 浏览器 18/18、B1 闭环 16/16；`mvn -q clean package -DskipTests`、`npm run lint`、`npm run build` 全绿。

**数据说明**：为 B1 验证，3 道 seed 题临时置为发布态（临时 `published_at`），B2 结束后还原为 seed 草稿态。

### T12 · 管理端题目 CRUD + 发布/下架 ✅

**改动文件**
- 后端（新增）：`admin/questions/{AdminQuestionController,AdminQuestionService,AdminQuestionMapper}.java`、`admin/dto/QuestionSaveReq.java`、`admin/vo/{AdminQuestionListItemResp,AdminQuestionDetailResp,QuestionIdResp,QuestionStatusResp}.java`、`question/entity/Question.java`
- 后端（修改）：`common/config/SaTokenConfig.java`（注册 `SaInterceptor` 启用方法级 `@SaCheckRole`）
- 前端（新增）：`src/views/admin/{AdminQuestions,AdminQuestionEdit}.vue`、`src/components/admin/QuestionForm.vue`、`src/api/admin.js`
- 前端（修改）：`src/router/index.js`（两条管理端路由换真实组件）

**验收（真实输出，2026-11-05，真实 DB + curl + headless Edge/CDP）**
- 列表/筛选：`GET /admin/questions` → `total=3`、字段齐全 `id,title,categoryId,categoryName,difficulty,status,source,publishedAt,updatedAt`；`status=0`→0、`status=1`→3、`keyword=HashMap`→1、`pageSize=500`→`size=50`
- 新建：`POST` → `{"id":"4"}`，DB `status=0`、`published_at` NULL、标签 2 行；草稿前台列表不出现、详情 404
- 详情/标签覆盖：`GET /admin/questions/4` → `tagIds=[1,3]`；改成 1 个 → `question_tag` 恰 1 行；再改 3 个 → 恰 3 行
- 发布：`PUT publish` → `{"status":1}`、`published_at` 非空；重复发布不改 `published_at`；前台列表/详情立即可见；**已发布题编辑保存后前台标题立即更新**
- 下架：`PUT offline` → `{"status":2}`、前台详情 404、列表不出现；重新发布后 `published_at` 仍为首次值、前台恢复可见
- 逻辑删除：`DELETE` → DB `deleted=1`、管理端列表不出现、前台 404
- 校验：缺标题 / 难度=5 / 分类 999 / 标签 11 个 / 标签 999 均 HTTP 400 `40000` 且文案定位字段；空答案发布 → HTTP 400 `40000`「参考答案不能为空」；`GET/DELETE /admin/questions/999` → 404
- 权限：普通用户 `GET/POST /admin/**` → HTTP 403 `40301`；无 token → 401 `40100`；管理员菜单可见；非管理员访问 `/admin/questions` 路由守卫跳 `/403`
- 浏览器：管理端入口 → 列表 3 行状态标签；新建页空表单 4 条校验错误；填标题/题干/答案/分类 Java/难度中等/标签 JVM；未保存点返回弹「当前修改尚未保存」并可继续编辑；保存草稿 → 列表出现「草稿」；发布 → 状态变「发布」→ 前台详情可见；编辑标题保存 → 前台立即生效；下架 → 前台 404；删除 → 列表消失；375×667 列表转卡片、编辑页无横向滚动
- `mvn -q clean package -DskipTests` exit 0；`npm run lint` / `npm run build` exit 0

**偏离登记**：见 `开发文档/02 §10`（写侧 Question 实体与辅助 VO、SaTokenConfig 启用注解鉴权、T12 暂用公开分类接口待 T13 切换、QuestionForm 增加 cancel emit）。

### T13 · 分类标签维护 + JSON 批量导入 ✅

**改动文件**
- 后端（新增）：`admin/taxonomy/{AdminCategoryController,AdminTagController,AdminTaxonomyService,AdminTaxonomyMapper}.java`、`admin/imports/{ImportController,ImportService,ImportResult,ImportMapper}.java`、`admin/dto/{CategorySaveReq,TagSaveReq,ImportReq}.java`、`admin/vo/{AdminCategoryResp,IdResp}.java`、`taxonomy/entity/{Category,Tag}.java`
- 前端（新增）：`src/views/admin/AdminTaxonomy.vue`、`src/components/admin/JsonImportDialog.vue`
- 前端（修改）：`src/api/admin.js`（分类/标签 CRUD + 导入）、`src/views/admin/AdminQuestions.vue`（分类下拉改管理端接口 + 批量导入入口）、`src/views/admin/AdminQuestionEdit.vue`（分类下拉改管理端接口）、`src/router/index.js`（`/admin/taxonomy` 换真实组件）

**验收（真实输出，2026-11-05，真实 DB + curl + headless Edge/CDP）**
- `GET /admin/categories` → 5 条，字段 `id,name,description,sort,status,questionCount,publishedCount`；Java `1/1`；新建草稿题后 Java `2/1`（**questionCount 含草稿、publishedCount 只算发布态**，口径可区分）
- 新建/改名/停用分类：停用后管理端表格仍返回 `status=0`（含编辑说明），公开 `GET /categories` 不再返回；删除被引用分类 Java → HTTP 409 `40901`「该分类下还有题目，不能删除；可改为停用」；删除空分类成功；重名 → 409 `40900`；删除不存在 → 404
- 标签：新建/改名成功；重名 409；删除被引用标签 → 409 `40901`「标签已被题目引用，需先解除关联」；删除未引用标签成功
- 导入 20 题（含 2 条重复）→ `{"created":18,"skipped":2,"failed":0}`，DB 题数 +18；顶层 category 自动创建（`NetImport`）、tags 自动创建（`TCP/UDP`），每题 2 条 question_tag（合计 36）；status=1 的 2 题前台列表可见
- 导入含 1 条缺 `answer_md` → `{"created":2,"failed":1,"errors":[{"index":1,"reason":"answer_md 不能为空"}]}`（index 从 0 开始，其余不受影响）
- 导入 51 题 → HTTP 400 `40000`「questions 数量必须为 1–50」，DB 题数不变（无入库）；非法 JSON → 400 `40000` 带解析信息；空 questions → 400
- 单题 `category` 覆盖顶层 → 入库到 MySQL（`override category = MySQL`）
- 浏览器：分类表 5 行 / 标签表 15 行；行内新增「E2E分类」→ 停用后管理端仍在、前台 `/categories` 不含；管理端题目筛选下拉显示「E2E分类（已停用）」；删除 Java → 40901 提示且仍在列表；对话框改名成功；删除空分类成功；标签新增/改名（prompt）/删除成功；导入对话框非法 JSON 就地提示「第 1 行第 2 列」；合法 JSON 显示「创建 2 条 · 跳过 0 条 · 失败 0 条」且列表出现导入题；375×667 分类/标签切 Tab、无横向滚动
- `mvn -q clean package -DskipTests` exit 0；`npm run lint` / `npm run build` exit 0

**偏离登记**：见 `开发文档/02 §10`（新增管理端 Mapper/实体与 ImportController、导入 status=1 写 published_at、逐条独立入库无整批事务、管理端下拉切换到 /admin/categories、编辑交互细节）。

### B2 检查点（T12–T13 管理端）✅

**浏览器全链路（headless Edge/CDP，真实接口与真实 DB）11/11 通过**
- 管理员登录 → 管理端入口 → 分类与标签页：行内新增「B2分类」/「B2标签」成功 → 题目管理页「批量导入」2 题（摘要「创建 2 条 · 跳过 0 条 · 失败 0 条」）→ 新建题（标题/题干/答案/分类 Java/难度中等）→ **「保存并发布」一步完成** → 列表状态=发布 → 前台 `/question/{id}` 标题与答案折叠按钮均正常 → 下架 → 前台 404 → 删除 → 管理端列表消失 → 停用分类后管理端仍在、前台 `/categories` 不含 → 375×667 管理端无横向滚动
- 修复项专项验证 13/13：admin 关键词匹配标签名（JVM → 1 条）、草稿不可直接下架（40000）、已发布可下架、停用分类带「已停用」文字、移动端「编辑/预览」Tab 可切换（默认编辑、点预览隐藏表单）、导入弹窗在 375 下宽 345px 无横滚、已发布题保存前弹「前台将立即更新」且取消不保存

**代码审查（code-review，fixed point `e2fc1f0` = T11 修复提交）**
- **Standards 轴** 7 项：已修 6 项——移动端编辑页无「编辑/预览」Tab（`mobileTab` 死状态，已补 el-tabs）、管理端 15 个写接口缺审计日志（已补 `audit action=… userId=… targetId=… result=OK`）、`offline` 不校验源状态（已限定 1→2，草稿下架返回 40000）、标签表无空态（已补 EmptyState/ErrorState）、`difficulty` 魔法值（已提常量）、`QuestionForm.submitting` 未登记（已登记 02 §10）；1 项判断保留（`countAdminPage` 与 `selectAdminPage` 的 WHERE 重复，注解 Mapper 无法共享 SQL 片段）。
- **Spec 轴** 确认 3 项 + 存疑 3 项：已修——已发布题保存前缺「前台将立即更新」确认（01 F09，已补确认框）、移动端 Tab（同上）、标签表空态/失败态（同上）、admin 关键词只匹配标题（04 #27 指向 #8，已改为标题或标签名）、分类表格无「已停用」文字（已补 el-tag）；判断处理——删除确认文案按 04 A3.9 #31（收藏用户也看不到）实现并在 02 §10 登记与 B4.10 的矛盾；`publishedCount` 展示按 04 A3.9 #35「仅用于展示」保留；scope creep 另清死分支（keyword 30 后判 120 恒假、source 50 截断不可达）与 `QuestionIdResp` 同构类。
- 另修：查询参数类型不匹配（如 `status=abc`）原先落 50000，`GlobalExceptionHandler` 增加 `MethodArgumentTypeMismatchException → 40000`。

**回归**：T12 浏览器 18/18、T13 浏览器 17/17、B1 全链 16/16、T09/T10/T11 浏览器 18/13/18 全绿；`mvn -q clean package -DskipTests`、`npm run lint`、`npm run build` 全绿。

**数据还原**：B1/B2 验证用的临时数据已全部还原为 seed 原状（3 道草稿题、6 条 question_tag、5 分类、15 标签、仅 admin 用户、无刷题/错题/收藏记录、view/practice 计数归零）。

---

## 【交接 · 会话 B】

- **已完成**：T08–T13（B1=T08-T11、B2=T12-T13 全部检查点通过）
- **提交记录**（`git log --oneline -8`，由新到旧）：
  ```
  <B2 修复 commit> fix(T12-T13): code-review 修复（B2）
  5291c27 feat(T13): 分类标签维护 + JSON 批量导入
  14f112e feat(T12): 管理端题目 CRUD + 发布/下架（含编辑预览与未保存确认）
  e2fc1f0 fix(T08-T11): code-review 修复（分页口径/错题SQL去重/自评loading/今日数/图表色/滚动）
  10818a4 feat(T11): 统计与图表（5 指标卡 + 折线/饼图）
  e31a5c5 feat(T10): 错题本（列表/标记掌握/彻底移除/去复习）
  c372997 feat(T09): 三档自评 + 刷题记录（事务内写记录/错题/计数）
  bb25674 feat(T08): 收藏（幂等收藏 / 我的收藏 / 详情收藏按钮）
  ```
- **文档偏离**：见 `开发文档/02 §10`（T08–T13 及两轮 code-review 新增约 30 条：辅助 VO/Entity、PageUtil、WR upsert 行别名、详情页今日数数据源、导入状态机与落点、分页失败表现、图表色 token、删除确认文案等）
- **已知问题 / 判断项**：
  1. 管理端列表 `countAdminPage` 与 `selectAdminPage` 的 WHERE 条件重复（注解 Mapper 无 SQL 片段共享，属可接受重复）。
  2. `@Schema` 注解全仓未使用（02 §5.7 文档要求，既有类一致；springdoc 默认关闭）。
  3. 取消收藏按「不做二次确认」实现（04 B4.6 括号语义「二次确认=关闭」，02 §10 已登记）。
  4. 已下架题在收藏/错题列表置灰、详情 404；逻辑删除题三处均不展示（已实测）。
  5. 「问助手」按钮属 T16，本段未实现（04 B4.3 操作行剩余项）。
- **测试**：`mvn -q -DskipTests package` 通过（T14/T15/T18 才落单测）；`npm run lint` / `npm run build` 绿。
- **PROGRESS.md**：已更新到 B2。
- **恢复起点（会话 C）**：T14 mock 上游 + 上游客户端；DB 为 seed 3 草稿题 + admin（与 A 段交接一致），AI Key 留空、全程 mock profile。

---

## 【会话 C】任务卡记录（T14–T20）

### T14 · mock 上游 + 上游客户端 ✅

**改动文件**
- 后端（新增）：`ai/{AiProperties,LlmStreamClient,UpstreamSseParser,PromptBuilder,UpstreamMsg}.java`、`mock/MockUpstreamServer.java`、`chat/dto/Msg.java`
- 后端（配置）：`resources/application.yml`（追加 `ai.*`）、`resources/application-mock.yml`、`resources/prompt/system.txt`
- 单测（新增）：`test/.../ai/UpstreamSseParserTest.java`、`test/.../support/MockUpstreamServerTest.java`

**验收（真实输出，2026-11-05）**
- `mvn -q -DskipTests compile` → exit 0
- `mvn -q test -Dtest=UpstreamSseParserTest,MockUpstreamServerTest` → exit 0；`UpstreamSseParserTest Tests run: 8, Failures: 0, Errors: 0` + `MockUpstreamServerTest Tests run: 10, Failures: 0, Errors: 0`（合计 18）
  - 覆盖：分段拼接、`finish_reason`、`usage`/`search_prompt_tokens`、`code!=0` 不算成功且不进入 delta、`reasoning_content` 忽略、`security_suggest=HIDE_CONTINUE` 跳过该段继续、usage 缺失 `estimated=1`、malformed 行跳过
  - `mock-ok` 收满 20 段 + 末 chunk `usage(total_tokens=32)` + `[DONE]`；请求体断言含 `"thinking":{"type":"disabled"}` 且无 `stream_options`
  - `mock-500` 首包前重试 1 次后成功（mock 服务端命中数 = 2）；`mock-429` 不重试、直接抛 `UpstreamHttpException(429)`
  - `mock-code`（HTTP 200 + body.code=11201）暴露上游 code 且 `hasDelta=false`；`mock-security` 隐藏片段不转发、正文照收 20 段
- mock profile 启动：`mvn spring-boot:run -Dspring-boot.run.profiles=mock` → `[mock] upstream started: http://127.0.0.1:18080/chat/completions`；`GET /api/v1/health` → `{"status":"UP","db":"UP",...}`
- `curl -N` 经 mock 上游逐行收流（`--trace-ascii --trace-time` 实测到达时间）：`19:24:03.327 / .461 / .584 / .709 / .834 … 19:24:05.826`（相邻约 125ms，不是一次性）；`-w` → `http=200 first_byte=0.001272s total=2.486057s`；末两行为 `...finish_reason":"stop"... "usage":{"prompt_tokens":12,...}` 与 `data: [DONE]`
- `ai.api-key` 在 mock 下取值 `mock`（`MockUpstreamServerTest.mockProfileStartsServerOn18080WithMockKey` 断言通过），不读真实 Key

**偏离登记**：见 `开发文档/02 §10`（`@SpringBootTest(profiles=)` 不存在改 `@ActiveProfiles`；mock 增加 `mock-code`/`mock-security` 两个验收剧本；T14 提前建 `chat/dto/Msg`；`application.yml` 补 `ai.*` 默认键；重试落在 `LlmStreamClient.open()`）

### T15 · 配额/限流 + `/ai/chat` SSE ✅

**改动文件**
- 后端（新增）：`chat/{ChatController,ChatService,ChatValidator,QuotaService,QuotaMapper,RateGate,AiStreamRegistry,AiChatLogService,AiChatLogMapper}.java`、`chat/dto/ChatReq.java`、`chat/vo/AiQuotaResp.java`、`chat/entity/AiChatLog.java`、`ai/{AiExecutorConfig,UpstreamCodeMapper}.java`
- 后端（修改）：`common/exception/GlobalExceptionHandler.java`（异步异常 + 流前 JSON 内容协商）、`mock/MockUpstreamServer.java`（`ai.mock.port` 随机端口支持，供集成测试）
- 单测（新增）：`test/.../chat/{ChatGateTest,ChatQuotaTest,ChatStreamTest}.java`、`test/.../support/ChatTestSupport.java`

**验收（真实输出，2026-11-05）**
- `mvn -q test -Dtest=ChatGateTest,ChatQuotaTest,ChatStreamTest` → exit 0；`ChatGateTest Tests run: 8, Failures: 0` + `ChatQuotaTest Tests run: 2, Failures: 0` + `ChatStreamTest Tests run: 5, Failures: 0`（15 个真实用例，覆盖 05 §12 #1–#13、#15）
  - #1 未登录 → HTTP 401 + 40100、无 `text/event-stream`
  - #2 `mock-ok`：`event:meta×1 → event:delta×20 → event:done×1`；`done.usage.totalTokens=32`；`done.quota.used=1/limit=30`；DB `used_count=1`、`site_ai_usage.used_tokens=32`；请求体含 `"thinking":{"type":"disabled"}` 且无 `stream_options`
  - #3 账号额度 30/30 → 429 + 42901，`GET /ai/quota` 同步 `used=30 remaining=0 resetAt=2026-11-06 00:00:00`
  - #4 4 条在途时第 5 条 → 429 + 42902，且第 5 条 `used_count=0`（闸门在预扣之前）
  - #5 `mock-slow` → 20s 后 `event:error{50202}`、`user_ai_quota.used_count=0`（回滚）、status=2；等 3s 后换 `mock-ok` 可继续成功
  - #6 `mock-break` → 收 3 段 delta 后 `event:error{50202}`、无 done、计 1 条、`ai_chat_log.estimated=1`
  - #7 客户端读到首个 delta 后断开 → 服务端 `ai_chat_log.status=3`、已收文本保留、额度不返还；Semaphore 释放，3s 后可再次成流
  - #8 11 条 / 7700 字符上下文 → **不报 40000**，静默裁剪为 ≤10 条且 ≤6000 字符，末条用户消息保留
  - #9 注入 `role=system` → 上游请求体只有服务端 system + user，注入内容被丢弃
  - #10 `questionId=1`（草稿）→ 流前 404 + 40400，未调用上游（`totalRequests=0`）
  - #11 同账号并发 → 第 2 条 42901；#12 3s 内第 2 条 → 42901 且额度不变；#13 `mock-nousage` → `done.usage.estimated=1` 不中断；#15 线程池拒绝 → 42902 + 两项额度回滚（Mockito 构造）
- 真实服务手动验证（mock profile，`curl -N` 走 8080）：登录 admin → `POST /api/v1/ai/chat` → `http=200 first_byte=0.029499s total=2.702091s`，事件计数 `meta 1 / delta 20 / done 1`，末行 `data:{"messageId":"9a514375","usage":{"promptTokens":12,...,"totalTokens":32,"estimated":0},"quota":{"limit":30,"used":1}}`
- 连续两次请求（第一次 2.49s 结束、立即发第二次，距第一次启动 <3s）→ `{"code":42901,...}` + HTTP 429，`used_count` 保持 4
- 客户端 `--max-time 0.8` 中途断开 → `ai_chat_log` 新增 `status=3, estimated=1, latency_ms=865`，`used_count` 从 3 变 4（已出字计 1 条不回滚）
- `GET /api/v1/ai/quota` → `{"dailyLimit":30,"used":3,"remaining":27,"siteRemaining":297,"resetAt":"2026-11-06 00:00:00"}`
- 验证后清理：`user_ai_quota` / `site_ai_usage` / `ai_chat_log` 清空、测试临时用户删除（仅剩 admin）

**说明**：测试用 `@SpringBootTest(webEnvironment=RANDOM_PORT)` + JDK HttpClient 读真实字节流（MockMvc 对 SseEmitter 的 `asyncDispatch` 会永久等待，已登记 02 §10）；mock 上游在本用例上下文用随机端口并回写 `ai.base-url`，不与 T14 的 18080 冲突。

**偏离登记**：见 `开发文档/02 §10`（SSE 单测方式、mock 随机端口、新增 Mapper/实体/出参/映射类、流前 JSON 内容协商、客户端断开异常、sessionId 入参、complete 时机、闸门间隔推进、已出字失败按估算计 tokens）












