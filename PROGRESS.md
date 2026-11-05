# PROGRESS · 码上记（CodeInterview）

> 执行模式：开发 Agent 全权执行（站长不参与）：自己建库、自己验收、自己 commit、自己进下一张卡。
> 规范真源：`开发文档/`（README + 01–07）。本文件只记录**执行进度与真实验收输出**，不重复定义规范。

## 检查点进度

| 检查点 | 卡片 | 状态 |
|---|---|---|
| A1 工程基建 | T01 / T02 / T03 | ✅ 通过（health UP、404 走 handler、参数 400、seed 入库） |
| A2 登录鉴权 | T04 | ✅ 通过（注册/登录/刷新保持/登出/改名改密/锁定） |
| A3 题库只读 | T05 / T06 / T07 | ✅ 通过（首页/列表/详情/代码高亮/登录/刷新保持，浏览器实测） |

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










