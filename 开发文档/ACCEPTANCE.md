# 码上记 · 验收用例映射表（AC-01 ~ AC-17）

> 真源：`01_码上记_PRD&需求边界.md` §11。本文件把每条 AC 映射到「哪张卡验证 + 可重复的命令/操作」，
> 替代口头验收。最后一列为 **T20 交付前复核结果**（2026-11-05，全部真实执行）。
>
> 通用前置：
> - 本机 MySQL80（3306）已启动；库 `codeinterview` 已执行 `db/init.sql` + `db/seed-demo.sql`；
> - 后端：`cd backend && mvn spring-boot:run`（无真实 AI Key 时用 `-Dspring-boot.run.profiles=mock`）；前端：`cd frontend && npm run dev`；
> - 部署验收：`nginx/default.conf`（SPA 回退 + `/api` 反代 + SSE 独立 location）+ 单 jar，见 README §7。

| 编号 | 用例 | 负责卡 | 验证命令 / 操作 | T20 复核 |
|---|---|---|---|---|
| AC-01 | 游客：首页 → 列表 → 详情 → 展开答案 | T05/T06/T07/T18 | `curl -s localhost:8080/api/v1/home`；`curl -s "localhost:8080/api/v1/questions?page=1&pageSize=10"`；浏览器打开 `/` → 点分类 → 点题目 → 点「展开参考答案 ▾」 | ✅ 通过（首页 latest/hot 各 6、题库 total=18、详情 101 代码高亮） |
| AC-02 | 注册 → 登录 → 刷新保持 → 退出 | T04/T19 | 注册 `POST /api/v1/auth/register {nickname,password}` → 登录 → 带 token `GET /api/v1/auth/me` → 关闭标签页重开（sessionStorage 失效）或 `POST /auth/logout` 后再 `GET /auth/me` | ✅ 通过（register/me/logout 200，登出后 401；前端刷新保持见 T04/T19） |
| AC-03 | 关闭注册开关后注册 | T04/T18 | 以 `APP_REGISTER_ENABLED=false` 启动 → `POST /api/v1/auth/register` | ✅ 通过（HTTP 403 + `40300`「注册已关闭，请联系管理员」） |
| AC-04 | 自评「不会」→ 错题本 → 模糊 → 掌握 → 移出 → 再不会 | T09/T10/T18 | `POST /questions/101/assessment {selfAssess:3/2/1, source:1}` 逐步调用；或 `mvn -q test -Dtest=AssessmentStateMachineTest` | ✅ 通过（wrongBook `0/1 → 0/2 → 1/2 → 0/3`；单测 2/2） |
| AC-05 | 错题本「去复习」→ 自评 | T09/T10/T18 | `POST /questions/102/assessment {selfAssess:2, source:3}` → `GET /practice-records` 过滤该题 `source=3` | ✅ 通过（source=3） |
| AC-06 | 收藏 → 列表可见 → 取消 → 列表消失 | T08/T18 | `POST/DELETE /questions/103/favorite` + `GET /favorites`；DB `SELECT COUNT(*) FROM user_favorite` | ✅ 通过（before=1→after=0，无残留行） |
| AC-07 | 管理员新建 → 预览 → 发布 → 游客可见 → 下架 → 游客 404 | T12/T13/T18 | 管理端新建并 `PUT /admin/questions/{id}/publish` → 游客 `GET /questions/{id}` → `PUT …/offline` → 再 `GET` | ✅ 通过（发布 200 → 下架后 404） |
| AC-08 | 导入 20 题 JSON → 汇总 → 逐题发布 | T13/T18 | `POST /admin/questions/import {"json":"{\"category\":…,\"questions\":[…20]}"}`；或 `mvn -q test -Dtest=ImportServiceTest` | ✅ 通过（created=20；单测 4/4） |
| AC-09 | 非法 JSON / 超 50 题 / 单条缺答案 | T13/T18 | 同上接口传 `{bad`、51 题、缺 `answer_md` 的单条 | ✅ 通过（400/400；created=1 failed=1 `errors[0].index=1`） |
| AC-10 | 助手提问，观察流式与首字时间 | T14/T15/T16/T18 | 浏览器详情页「问助手」→ 发送；或 `curl -N` 打 `/api/v1/ai/chat`（mock-ok） | ✅ 通过（20 delta+done；经 Nginx 38 chunk，首 delta 71ms） |
| AC-11 | 详情页「问助手」后追问两轮 | T15/T16 | 带 `questionId` 发两轮 `messages`（含上一轮 assistant）；确认服务端 prompt 只按 questionId 查库拼题干 | ✅ 通过（两轮 200+done；题干拼接见 `PromptBuilder`，mock 上游不体现语义） |
| AC-12 | 连续发送触发 3s 间隔 / 并发 / 日额度 / 全站预算 | T15/T16/T17 | 同账号 3s 内二次请求；`mvn -q test -Dtest=ChatGateTest`（#3/#4/#11/#12） | ✅ 通过（3s 内 429+42901；并发/额度/全站 42902 见单测） |
| AC-13 | 断网 / 上游失败（未出字 / 部分输出） | T15/T16 | `mock-slow` → 20s 50202+回滚；`mock-break` → error/未完成、计 1 条；浏览器停服 → 「未完成+重新生成」 | ✅ 通过（单测 + T16 浏览器） |
| AC-14 | 越权访问他人错题/收藏/记录；普通用户访问 `/admin/**` | T10/T17/T18 | 普通用户 token 调 15 个管理端接口；A 用户 `PUT /wrong-book/{B 的题}/resolved` | ✅ 通过（15/15 403+40301；跨用户 403+40300） |
| AC-15 | 按 README 部署（Nginx + jar + MySQL） | T18 | 见 README §7：Nginx 8088 + jar + 本机 MySQL；`/health`、首页 18 题、SSE 逐字、`scripts/backup.sh` 可恢复 | ✅ 通过（部署全链路 + 备份恢复 11 表/21 题；重启后 token 失效需重登） |
| AC-16 | 所有 Markdown 输出经 DOMPurify；XSS 不执行；id 为字符串 | T06/T11/T15/T16/T18 | 详情/答案/助手定稿走 `MarkdownView`；管理员建含 `<script>/<img onerror>` 的题并打开详情；API 检查 id 类型 | ✅ 通过（DOM 中 onerror=0/script=0；id 均 string；`check.sh` 校验 v-html 唯一入口） |
| AC-17 | 375×667 与 1440×900 全链路；管理端移动端可用 | T19 | 14 路由 × 4 视口巡检：`document.documentElement.scrollWidth ≤ 视口`；抽屉/底部筛选/chips/表格转卡片/Tab/浮窗全屏/横屏/软键盘；`bash scripts/check.sh` | ✅ 通过（375/414/768/1440 各 `14/14` 无横向滚动；抽屉/筛选/chips/卡片/Tab/浮窗全屏均实测） |

## 已知限制（不影响验收结论）

1. **真机一次待站长**：Agent 无物理手机，AC-17 用 headless Edge 设备模拟（375/414/768/1440 + 触屏）覆盖；部署后请站长用手机访问 `http://<本机IP>:80` 走一遍登录→自评→浮窗。
2. **AI 语义验证**：全程 mock 上游（无真实 Key）。AC-11「回答能引用题干」在 mock 下只验证结构链路（questionId → 服务端查库拼 system → 两轮上下文），语义由 `05 §13` 的 prompt 约定保证；拿到真实 Key 后建议手工抽查一次。
3. **部署 profile**：T18 部署验收用 `--spring.profiles.active=mock` 启动 jar（本机无真实 Key），SSE 链路与真实 Spark 相同；生产用 `prod` + `.env`。
4. **Dockerfile**：已交付但本机 Docker 守护进程不可用，未执行 `docker build`；V1 部署走 Nginx + jar。

## 交付前自查命令（T20）

```bash
# 1) 后端全量单测（39 个，AI 上游走 mock，不读真实 Key）
cd backend && mvn -q test

# 2) 前端静态检查与构建
cd frontend && npm run lint && npm run build

# 3) 静态红线（id 转数字 / v-html 唯一入口 / 依赖红线 / 密钥泄露 / 构建体积）
bash scripts/check.sh

# 4) 全量 AC：按本表逐条执行；AI 用例需 mock profile
```
