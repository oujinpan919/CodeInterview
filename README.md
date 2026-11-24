# 码上记 CodeInterview

面试题库 + 三档自评 + 间隔重复复习（今日计划）+ 私人笔记 + 刷题路线 + AI 对话助手（SSE 流式）。Vue3 SPA + Spring Boot 3 单体 + MySQL 8。

- 前端：Vue 3.5 + Vite 8 + Element Plus + Pinia 3 + vue-router 4（JavaScript，非 TypeScript）
- 后端：Spring Boot 3.5.16 + JDK 17 + MyBatis-Plus 3.5.18 + Sa-Token 1.46.0 + MySQL 8.0.46
- 架构：前后端分离、单 jar 单体；无 Redis / MQ / ES

开发与接口规范见 `开发文档/`（`README.md` 为索引，`02` 技术栈、`03` 数据库、`04` 接口与 UI、`06` 任务卡）。

---

## 1. 目录结构

```
CodeInterview/
├── backend/                 # Spring Boot 单体工程（Maven）
│   ├── config/              # 本机密钥目录（application-local.yml 不入库）
│   └── src/main/java/com/codeinterview/
├── frontend/                # Vue 3 SPA（独立 npm 工程）
├── db/
│   ├── init.sql             # 唯一建表真源（15 张表 + 分类/标签/示例草稿题 seed）
│   ├── seed-demo.sql        # 演示数据（18 道发布题 + admin/demo 账号 + 自评/错题/收藏）
│   ├── migration-v1-expansion.sql  # 增量建表（第 12 张表 + 4 分类 + 15 标签；现有库升级用）
│   ├── seed-extra.sql       # 补充 200 道发布态题（id 201–400）+ 4 分类
│   ├── seed-extra-500.sql   # 再补 500 道发布态题（id 401–900，含题目标签）
│   ├── migration-v2-review.sql  # v3.6 批次一：复习排程三列 + 回填（幂等）
│   ├── migration-v3-note.sql    # v3.6 批次三：第 13 张表 question_note（幂等）
│   ├── migration-v4-path.sql    # v3.6 批次六：第 14/15 张表 + 4 条路线 + 102 题项（幂等）
│   ├── migration-v5-ai-config.sql  # v3.20：7 张 AI 表 + ai_chat_log 扩 4 列 + seed（幂等）
│   ├── migration-v6-question-view.sql # v4.x：第 16 张表 question_view（浏览记录，幂等）
│   ├── migration-v7-category-split.sql # v4.5：9 分类 → 26 细分方向 + 718 题归位（幂等）
│   ├── migration-v8-taxonomy-align.sql # v4.6：对齐参考站粒度，44 分类骨架 + MySQL/Redis 等合并（幂等）
│   ├── seed-technology-talk.sql      # v4.7：从 technology-talk 导入 266 道技术题（幂等）
│   ├── seed-technology-talk-tags.sql # v4.7：为导入题补标签 + 9 个新标签（幂等）
│   ├── seed-ai-templates.sql    # v3.22.3：讲解风格模板（保留 id 4/6，停用 id 3/5，幂等）
│   └── grant.sql.example    # 应用账号与授权模板（复制为 grant.sql 后填密码，不入库）
├── docker/                  # MySQL 8.4 容器备选（映射 3307，默认不用）
├── nginx/default.conf       # 部署用 Nginx（SPA 回退 + /api 反代 + SSE 独立 location）
├── backend/Dockerfile       # 后端单 jar 运行镜像
├── scripts/backup.sh        # MySQL 备份（mysqldump + 日期文件名 + 保留 14 天）
└── 开发文档/                # 01–07 开发规范与任务卡
```

## 2. 前置要求

| 组件 | 版本 | 本机实测 |
|---|---|---|
| JDK | 17 | `D:\Develop\jdk\jdk-17`（`JAVA_HOME` 已指向） |
| Maven | 3.9.x | `D:\Develop\apache-maven-3.9.16`（未加入 PATH，需写全路径） |
| MySQL | 8.0.46 | 服务 `MySQL80` 运行中，端口 `3306`；客户端 `D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe` |
| Node.js | 24 LTS | `D:\Develop\nodejs`（`npm` 11.x） |

> Node 有 PATH 陷阱：当前 shell 的 `node -v` 可能解析到 22.x。装依赖/起前端时用绝对路径
> `& "D:\Develop\nodejs\npm.cmd" ci`，或把 `D:\Develop\nodejs` 提到 `$env:Path` 最前，确保 `node -v` 输出 `v24.x`。

## 3. 快速开始（本机）

### 3.1 初始化数据库（只做一次）

```powershell
# 1) 建库 + 12 张表 + seed（root 执行；--default-character-set 保证 seed 中文不乱码）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/init.sql"

# 2) 演示数据（18 道发布题 + admin/demo 账号 + 自评/错题/收藏）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-demo.sql"

# 3) 增量建表脚本（第 12 张表 + 4 分类 + 15 标签；全新库下 CREATE 为 no-op）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v1-expansion.sql"

# 3.1) 复习排程 / 私人笔记 / 刷题路线（v2–v4，幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v2-review.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v3-note.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v4-path.sql"

# 4) 补充 200 道发布态题（id 201–400）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-extra.sql"

# 5) 再补 500 道发布态题（id 401–900，含标签）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-extra-500.sql"

# 5.1) AI 配置表 / 题库浏览记录 / 用户头像（v5、v6、v9；v5 必须先于讲解模板）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v5-ai-config.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v6-question-view.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v9-user-avatar.sql"

# 5.5) v3.22.3：讲解风格模板（启用 口述答题版 / 源码级剖析；极简速记 / 对比辨析 已停用）
#      依赖 migration-v5-ai-config.sql 已建 ai_prompt_template；幂等，可重复执行
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-ai-templates.sql"

# 5.6) v4.5：题库分类细化（9 个宽分类 → 26 个细分方向，718 道题按标签/人工规则归位）
#      ⚠️ 必须在 seed-extra-500.sql 之后执行 —— 它要把题目从旧的 9 个分类搬到新分类；
#      早于 seed 执行会搬空表。幂等，可重复执行。
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v7-category-split.sql"

# 5.7) v4.6：分类骨架对齐参考站（44 个分类；MySQL/Redis/计算机网络合并回单分类，
#      并补齐 AI/Go/C++/Python/MyBatis/Netty/运维 等空分类等题库灌入）。幂等。
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v8-taxonomy-align.sql"

# 5.8) v4.7：导入 technology-talk 技术题 266 道 + 补标签（幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-technology-talk.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-technology-talk-tags.sql"

# 6) 建应用账号并授权（root 执行；db/grant.sql 已被 gitignore）
Copy-Item db\grant.sql.example db\grant.sql
#    编辑 db/grant.sql，在两个 IDENTIFIED BY '' 里填入应用账号密码（本机开发为 123456）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/grant.sql"
```

> **全新安装全量顺序**（本机/演示；生产见 §7.1.0，禁止 seed-demo）：`init.sql` → `seed-demo.sql`（仅演示）→ `migration-v1-expansion.sql` → `migration-v2-review.sql` → `migration-v3-note.sql` → `migration-v4-path.sql` → `migration-v5-ai-config.sql` → `migration-v6-question-view.sql` → `migration-v9-user-avatar.sql` → `seed-extra.sql` → `seed-extra-500.sql` → `seed-ai-templates.sql` → `migration-v7-category-split.sql` → `migration-v8-taxonomy-align.sql` → `seed-technology-talk.sql` → `seed-technology-talk-tags.sql` → `grant.sql`。完整 **984 道发布态题 / 44 分类 / 39 标签 / 25 张表 / 4 条刷题路线 / 6 个讲解风格模板（启用 4 个）** 须按顺序全量执行；执行后以 `SHOW TABLES` 为准（当前 25），`/api/v1/health` = `UP`。
> **现有库升级**：先 `bash scripts/backup.sh` 全量备份；再按 §7.1.0 的 schema 顺序核对 `migration-v1`–`v6`、`v9` 是否已执行，**缺哪条补哪条**；数据 seed 按需执行；**绝不重跑 `init.sql`**（会撞唯一键并破坏既有用户数据）。
> **分类骨架（v4.5 + v4.6 两个脚本）**：`migration-v7-category-split.sql` 先把 9 个宽分类拆成 26 个细分方向并归位 718 道题；`migration-v8-taxonomy-align.sql` 再按参考站粒度合并（MySQL/Redis/计算机网络各并为 1 个、GC 与调优并入虚拟机、浏览器并入 JavaScript）并补齐 AI/Go/C++/Python/MyBatis/Netty/Git/Nginx/RabbitMQ/MongoDB/React/运维/HR 等 **44 个启用分类**（其中 23 个暂空，等题库灌入）。两个脚本都**必须在全部 seed 之后执行**，旧分类只置 `status=0` 不删除；v4.5 执行前会自建 `_bak_qcat_v7` 映射备份表。
> **后三步脚本需 U-01/U-03 落盘后方可执行**；重复执行 `seed-extra.sql` / `seed-extra-500.sql` 靠固定 id + `INSERT IGNORE` 幂等，恒 700 题（200 + 500）、30 标签（分类数以 v4.6 后的 44 个为准）。
> MySQL 客户端不在 PATH，必须写全路径；PowerShell 不支持 `<` 输入重定向，统一用 `mysql -e "source 绝对路径"` 执行脚本。
> 本机默认使用已安装的 MySQL80（3306），**不要**启动 `docker/docker-compose.yml`。

### 3.2 本机密钥文件（只做一次）

```powershell
Copy-Item backend\config\application-local.yml.example backend\config\application-local.yml
```

在 `backend/config/application-local.yml` 中填三项：数据库应用账号密码、管理员初始密码、AI Key（用助手时）。
该文件已在 `.gitignore` 中，**永远不要提交**；模板文件 `application-local.yml.example` 的值必须保持为空。
该文件由 `backend/src/main/resources/application.yml` 的 `spring.config.import: optional:file:./config/application-local.yml`
加载，**任何 profile 下都生效**（含 mock/prod 单测），不需要 export 环境变量。

### 3.3 启动后端

```powershell
cd backend
& "D:\Develop\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

健康检查（不包 `Result`，DB 不通时返回 HTTP 503 + `db=DOWN`）：

```powershell
curl.exe http://127.0.0.1:8080/api/v1/health
# {"status":"UP","db":"UP","version":"1.0.0","time":"2026-11-05 10:00:00"}
```

### 3.4 启动前端

```powershell
cd frontend
& "D:\Develop\nodejs\npm.cmd" ci
& "D:\Develop\nodejs\npm.cmd" run dev
# http://127.0.0.1:5173 （/api 由 Vite 代理到 8080，Spring 侧不开 CORS）
```

## 4. 管理员账号

- 管理员**不在 SQL 里创建**：后端启动时 `AdminInitializer` 检测库中是否存在 `role=1` 用户；
  不存在且配置了 `admin.init-password` 时创建，昵称固定 `admin`，密码用 BCrypt（cost=12）哈希存储。
- 本机 `local` profile：允许弱密码，默认 `admin` / `123456`（由 `backend/config/application-local.yml` 的
  `admin.init-password` 提供）。**给他人访问或上公网前必须先改密码，并清空 `init-password`。**
- `prod` profile：初始密码必须通过密码策略（6–32 位，非全同字符、非顺序递增、不等于昵称），否则拒绝建号并打 ERROR。
- 未配置 `admin.init-password`：跳过建号并打 WARN（不报错）。
- **演示数据**：执行 `db/seed-demo.sql` 会预先插入 `admin` / `123456` 与 `demo` / `123456`
  （密码为固定 BCrypt cost=12 哈希）。此时 `AdminInitializer` 检测到已有管理员会跳过建号，密码以 seed 中为准。

## 5. 端口

| 端口 | 用途 | 说明 |
|---|---|---|
| `8080` | 后端 HTTP | `GET /api/v1/health` 探活 |
| `5173` | 前端 Vite 开发服务器 | `/api` 代理到 8080 |
| `3306` | MySQL 本机实例 | 服务 `MySQL80`，默认数据源 |
| `3307` | MySQL 容器备选 | `docker compose up -d mysql` 时使用，应用需 `DB_PORT=3307` |
| `80` | Nginx（部署） | 托管 `frontend/dist` + 反代 `/api`，详见 T18 与 `nginx/default.conf` |

## 6. 构建与测试

```powershell
# 后端
cd backend
& "D:\Develop\apache-maven-3.9.16\bin\mvn.cmd" -q clean package -DskipTests   # 打 jar
& "D:\Develop\apache-maven-3.9.16\bin\mvn.cmd" -q test                        # 单测；先准备隔离测试库，见下方说明

# 前端
cd frontend
& "D:\Develop\nodejs\npm.cmd" run lint
& "D:\Develop\nodejs\npm.cmd" run build
```

> 后端测试使用 `src/test/resources/application-test.yml` 指向 `codeinterview_test`。
> 首次运行前执行 `powershell -ExecutionPolicy Bypass -File scripts/setup-test-db.ps1`
> 创建测试库与最小权限账号；**禁止直接对开发库/生产库跑 `mvn test`**
> （`ChatTestSupport` 会清空 AI 测试表，未隔离时会误删真实数据）。

## 7. 部署（单机 Nginx + 单 jar + 本机 MySQL）

> V1 部署形态：**Nginx 托管 `frontend/dist` + 反代 `/api` 到后端 + 本机 MySQL 单实例**。
> 部署后**仅站长一人访问**（本机或局域网 HTTP），**不做 HTTPS / 域名 / 备案 / 公网开放**（V1.5 才做）。
> 参考 `nginx/default.conf`、`backend/Dockerfile`、`.env.example`、`scripts/backup.sh`。

### 7.1.0 生产初始化（推荐；禁止 seed-demo）

> 仅在全新空库执行；**不要执行 `seed-demo.sql`**。管理员由后端根据 `ADMIN_INIT_PASSWORD` 创建，
> 密码必须 ≥12 位且非弱口令；prod 启动时若检测到 admin/123456 或 demo 账号会直接拒绝启动。

```powershell
$mysql = "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe"
$db = "D:/Develop/Agent/projects/CodeInterview/db"

# 1) 建库 + 基础 schema（仅空库）
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/init.sql"

# 2) 全部增量 schema（顺序不可乱；v7/v8 必须放在数据 seed 之后）
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v1-expansion.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v2-review.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v3-note.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v4-path.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v5-ai-config.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v6-question-view.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v9-user-avatar.sql"

# 3) 题库/模板数据 + 分类迁移（顺序：数据 seed → v7 → v8 → technology-talk）
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/seed-extra.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/seed-extra-500.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/seed-ai-templates.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v7-category-split.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/migration-v8-taxonomy-align.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/seed-technology-talk.sql"
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/seed-technology-talk-tags.sql"

# 4) 应用账号（复制 grant.sql.example 后填强密码）
& $mysql -uroot -p --default-character-set=utf8mb4 -e "source $db/grant.sql"
```

### 7.1 演示环境一次性准备（仅本机；生产见 §7.1.0）

> ⚠️ 本节包含 `seed-demo.sql`（admin/demo 已知口令 123456），**严禁用于生产**。

```powershell
# 1) 建库 + schema + seed（root 执行；仅为演示）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/init.sql"

# 2) 演示数据（18 道发布题 + admin/demo 账号 + 自评/错题/收藏；在 init.sql 之后执行一次）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-demo.sql"

# 3) 增量建表脚本（第 12 张表 + 4 分类 + 15 标签；全新库下 CREATE 为 no-op）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v1-expansion.sql"

# 3.1) 复习排程 / 私人笔记 / 刷题路线（v2–v4，幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v2-review.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v3-note.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v4-path.sql"

# 4) 补充 200 道发布态题（id 201–400）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-extra.sql"

# 5) 再补 500 道发布态题（id 401–900，含标签）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-extra-500.sql"

# 5.1) AI 配置表 / 题库浏览记录 / 用户头像（v5、v6、v9；v5 必须先于讲解模板）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v5-ai-config.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v6-question-view.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v9-user-avatar.sql"

# 5.5) v3.22.3：讲解风格模板（id 3–6，幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-ai-templates.sql"

# 5.6) v4.5：题库分类细化（9 宽分类 → 26 细分方向；必须在所有 seed 之后，幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v7-category-split.sql"

# 5.7) v4.6：分类骨架对齐参考站（44 个分类，幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/migration-v8-taxonomy-align.sql"

# 5.8) v4.7：导入 technology-talk 技术题 266 道 + 补标签（必须在 5.7 之后，幂等）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-technology-talk.sql"
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-technology-talk-tags.sql"

# 6) 应用账号与授权（db/grant.sql 已被 gitignore）
Copy-Item db\grant.sql.example db\grant.sql   # 填密码后：
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/grant.sql"
```

> **现有库升级（重要）**：`init.sql` 仅适用于全新空库。现网升级：先全量备份；按 §7.1.0 核对 `migration-v1`–`v6`、`v9` 是否已执行，**缺哪条补哪条**；数据 seed 按需；**绝不重跑 `init.sql`**。
> 全量顺序见 §7.1.0（生产）与 §3.1（本机演示）；生产禁止执行 seed-demo。完整库以 `SHOW TABLES` 为准（当前 25 张表）。

### 7.2 构建产物

```powershell
# 前端：产物在 frontend/dist
cd frontend; & "D:\Develop\nodejs\npm.cmd" ci; & "D:\Develop\nodejs\npm.cmd" run build

# 后端：产物是 backend/target/codeinterview-backend-1.0.0.jar
cd backend; & "D:\Develop\apache-maven-3.9.16\bin\mvn.cmd" -q clean package -DskipTests
```

### 7.2.1 分类图标（生成产物，勿手改）

首页/题库页每个题库分类的图标由脚本生成，产物是 `frontend/src/data/categoryIcons.js`：

```powershell
# 首次生成前先装包围盒依赖（只用于脚本，不进前端产物）
cd frontend; npm install --no-save svg-path-bbox; cd ..
node scripts/_build-category-icons.mjs   # 生成图标数据
node scripts/test-category-icons.mjs     # 校验：结构合法 / 尺寸一致 / 44 分类全命中 / 无重复图形
```

图标来自三个可商用图标库：**Simple Icons**（CC0-1.0，品牌 logo）、**Phosphor**（MIT，抽象概念实心图标）、
**Devicon**（MIT，语言纯图形）。源文件缓存在 `scripts/_icons/`（已 `.gitignore`），缺失时脚本会自动联网下载，
因此新环境直接跑即可复现。要换某个分类的图标，改脚本里的 `SPEC` 再重新生成，不要直接编辑 `categoryIcons.js`。

归一化按**真实图形包围盒**缩放（不是 viewBox）——各图标库自身留白差异很大，按 viewBox 缩放会让图标大小不一。
所有图标最终可见尺寸统一为 24 画布内的 21.6 单位，校验脚本会断言这一点。

### 7.2.2 交付截图托管（阿里云 OSS）

`deliverables/` 下的界面截图统一上传到阿里云 OSS，方便在文档、Issue、PR 里直接引用：

```
https://codeinterview.oss-cn-shenzhen.aliyuncs.com/codeinterview/screenshots/<文件名>
```

Bucket `codeinterview`（华南1 深圳，`oss-cn-shenzhen.aliyuncs.com`），读写权限为**公共读**，
因此上述 URL 可直接外链、长期有效。上传脚本：

```powershell
# 凭证只走环境变量，不要写进脚本或提交到仓库
$env:OSS_ACCESS_KEY_ID     = "..."
$env:OSS_ACCESS_KEY_SECRET = "..."
python scripts/upload-deliverables-oss.py              # 上传 deliverables/assistant-ui/ 下所有图片
python scripts/upload-deliverables-oss.py --sign 86400 # 额外打印有效期 1 天的签名 URL
```

依赖：`pip install oss2`。可用 `--dir` / `--prefix` 指定其他本地目录和 OSS 路径前缀。

> 公共读意味着**任何人拿到 URL 都能访问**，且流量会计费。只放界面截图这类非敏感内容；
> 如果以后要放敏感素材，请改用私有读 + CDN 私有回源，或上传时按需生成签名 URL。

### 7.3 配置 Nginx

1. 把 `frontend/dist/*` 拷到 Nginx 静态目录（配置里的 `root`，默认 `/usr/share/nginx/html`）；
2. 把 `nginx/default.conf` 放到 `conf.d/`（或 `sites-enabled/`）；
3. 直接装 Nginx 时把 `upstream` 的 `backend:8080` 改成 `127.0.0.1:8080`；用 Docker 时保持服务名；
4. `nginx -t && nginx -s reload`。

> 三个必配项（少一个就会出问题）：SPA `try_files … /index.html`、`/api` 反代、
> **SSE 独立 location**（`proxy_buffering off` + `gzip off` + `proxy_read_timeout 300s`）；
> 另外 `server` 段必须下发 `X-Forwarded-For`，否则登录锁定/读接口限流会把全站当成一个 IP。

### 7.4 启动后端

```powershell
# 方式 A：直接跑 jar（工作目录放 backend；只绑回环，配合本机 Nginx）
cd backend
java -Xms256m -Xmx512m -jar target/codeinterview-backend-1.0.0.jar --server.address=127.0.0.1

# 方式 B：Docker（前端仍由 Nginx 托管；只把端口发布到宿主机回环）
cd backend
docker build -t codeinterview-backend:1.0.0 .
docker run -d --name codeinterview-backend --env-file ../.env -p 127.0.0.1:8080:8080 codeinterview-backend:1.0.0
```

生产环境用**环境变量**提供凭据（`.env.example` 为模板，复制为 `.env` 且不入库）：
`DB_HOST/DB_PORT/DB_USERNAME/DB_PASSWORD`、`ADMIN_INIT_PASSWORD`、`AI_*`、`SPRING_PROFILES_ACTIVE=prod`。
**v3.20 新增 `APP_ENCRYPT_KEY`**（用户凭证主密钥，32 字节 Base64 或 64 位十六进制）：
**prod 必须显式配置，缺失时启动直接失败**；仅 local/dev/mock 会自动生成并写入
`backend/config/application-local.yml`。
**换机或从备份恢复数据库前必须备份该密钥**，否则已存的用户凭证将无法解密
（管理员可在 `/assistant/settings` 页导出）。
环境变量优先级最高，会覆盖 jar 内 `application.yml`；服务器上不需要 `application-local.yml`。

> **AI 助手**：不配置 `AI_API_KEY` 时 `ai.enabled` 仍为 `true`，但 `/api/v1/ai/chat` 返回 50201（其余功能正常）。
> **v3.20**：用户可在 `/assistant/settings` 自带凭证接入任意 OpenAI 兼容模型；
> 无任何配置时回退 `ai.*` 全局兜底（`GLOBAL_FALLBACK`）。
> 本机演示可用 `--spring.profiles.active=mock`（内置 mock 上游，不需要真实 Key）。

### 7.5 部署后验证

```powershell
curl.exe http://127.0.0.1/api/v1/health                 # {"status":"UP","db":"UP",...}
# 浏览器打开 http://127.0.0.1/ → 首页应看到 984 道发布态题、44 个分类入口（AC-E28）、登录后「今日计划」卡与 /paths 刷题路线，点带代码的题有高亮
# 登录：
#   - 演示环境（执行过 seed-demo）：admin / 123456
#   - 生产环境：使用 ADMIN_INIT_PASSWORD 设置的 ≥12 位强密码；禁止执行 seed-demo
# 管理端入口可用；刷题记录页有指标卡与两张图
# SSE 经 Nginx 必须逐字输出（不是一次性）：
curl.exe -N -X POST http://127.0.0.1/api/v1/ai/chat -H "satoken: <token>" `
  -H "Content-Type: application/json" -H "Accept: text/event-stream" `
  -d "{\"messages\":[{\"role\":\"user\",\"content\":\"HashMap 扩容\"}]}"
```

### 7.6 备份与恢复

```bash
bash scripts/backup.sh          # 生成 backups/codeinterview_<yyyyMMdd_HHmmss>.sql.gz，保留 14 天
gunzip -c backups/codeinterview_20261105_030000.sql.gz | mysql -uroot -p codeinterview   # 恢复
```

计划任务示例（每天 03:00）：`0 3 * * * cd /path/to/CodeInterview && bash scripts/backup.sh >> logs/backup.log 2>&1`。

> 注意：Sa-Token 是**内存会话**，重启后端后所有 token 失效，需要重新登录（已在验收 AC-15 声明）。

## 8. 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| `/api/v1/health` 返回 503、`db=DOWN` | MySQL 未启动或库/账号未建：确认 `MySQL80` 服务 Running、已执行 `db/init.sql`、`application-local.yml` 里密码与应用账号一致 |
| 升级后 `/assistant/settings` 保存报 `Table 'xxx' doesn't exist` | 未执行 `db/migration-v5-ai-config.sql`（v3.20 新增 7 表 + `ai_chat_log` 扩 4 列）。执行后重启 |
| 用户配置页提示「主密钥可能已变更，请重新填写该凭证」 | `APP_ENCRYPT_KEY` 与加密时不同（常见于换机/恢复备份）。用管理员「显示/备份主密钥」取回原值，或让用户重填凭证 |
| 配置保存报「不允许连接内网或保留地址」 | SSRF 防护生效（08 §7 S1）。**生产禁止**把 `AI_ALLOW_PRIVATE_NETWORK` 置 true——那会放开内网探测面（云元数据地址仍会被单独拦截） |
| 启动报 `Access denied for user 'codeinterview'` | 应用账号未创建或密码不符：按 3.1 用 root 重建账号并 `FLUSH PRIVILEGES` |
| 分类/标签/题目 seed 中文乱码 | `mysql` 客户端字符集不对：执行时加 `--default-character-set=utf8mb4`，删库重建后重跑 `db/init.sql` |
| 8080 端口被占用 | 先停掉旧进程：`Get-NetTCPConnection -LocalPort 8080` 找到 PID 后 `Stop-Process` |
| 启动后报 `Port 50832 was already in use`（或端口不是 8080） | 环境变量 `SERVER__PORT`（**双下划线**，Spring 松散绑定等价于 `server.port`）覆盖了 `application.yml` 的 `server.port`。启动时显式覆盖：`java -jar target/codeinterview-backend-1.0.0.jar --server.port=8080`；命令行参数优先级高于环境变量。 |
| `mvn` 不是内部或外部命令 | Maven 未加入 PATH，用全路径 `D:\Develop\apache-maven-3.9.16\bin\mvn.cmd` |
| `npm ci` 报 engines 不支持 | `node -v` 不是 v24：按第 2 节处理 PATH，或用 `D:\Develop\nodejs\npm.cmd` 绝对路径 |
| 访问不存在路径返回 404 | 正常行为：`{"code":40400,"message":"内容不存在或已下架","data":null}`（HTTP 404，由全局异常处理） |
| 想用容器 MySQL | `cd docker && docker compose up -d mysql`，容器映射 3307；应用侧设 `DB_PORT=3307`，默认不要启用 |
| 前端刷新非首页路由 404（部署） | Nginx 必须配 `try_files $uri $uri/ /index.html`，见 `nginx/default.conf` |
| 误对现有库重跑 `db/init.sql` 毁数据 | **立即停止写入**，用 `scripts/backup.sh` 的最近全量备份恢复（`gunzip -c backups/codeinterview_<时间戳>.sql.gz \| mysql -uroot -p codeinterview`，见 §7.6）；恢复后**只跑** `migration-v1-expansion.sql` + `seed-extra.sql` 两条增量脚本，绝不再次重跑 `init.sql` |

## 9. 安全与配置约定（交付前检查）

- `backend/config/application-local.yml`、`db/grant.sql`、`.env` 均被 `.gitignore` 忽略，**不得提交**；
  模板文件 `application-local.yml.example` 的密钥字段必须为空。
- 日志、接口响应、前端代码中不得出现密码、token、AI Key（讯飞 Key 前缀是 `ak-`）。
  **v3.20 变更（D3）**：系统 Key（`ai.api-key`）仍**不入库**；用户在配置页填写的自有凭证
  按 AES-256-GCM 加密后落 `ai_credential` 表，接口只回掩码，日志不打印明文。
  主密钥来自环境变量 `APP_ENCRYPT_KEY`；prod 缺失时启动失败，local/dev 才会自动生成并写入 `backend/config/application-local.yml`。
- 统一返回 `Result`（仅 `/api/v1/health` 与 `/api/v1/export/notes` 两个探针/文件接口例外），业务错误 HTTP 状态同步，Long 类型 id 出参为字符串。
- 数据库账号只授权 `codeinterview.*`，应用不以 root 连接。
