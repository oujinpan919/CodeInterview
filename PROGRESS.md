# PROGRESS · 码上记（CodeInterview）

> 执行模式：开发 Agent 全权执行（站长不参与）：自己建库、自己验收、自己 commit、自己进下一张卡。
> 规范真源：`开发文档/`（README + 01–07）。本文件只记录**执行进度与真实验收输出**，不重复定义规范。

## 检查点进度

| 检查点 | 卡片 | 状态 |
|---|---|---|
| A1 工程基建 | T01 / T02 / T03 | ✅ 通过（health UP、404 走 handler、参数 400、seed 入库） |
| A2 登录鉴权 | T04 | ✅ 通过（注册/登录/刷新保持/登出/改名改密/锁定） |
| A3 题库只读 | T05 / T06 / T07 | 🚧 进行中 |

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



