# 码上记 CodeInterview

面试题库 + 三档自评 + 错题复习 + AI 对话助手（SSE 流式）。Vue3 SPA + Spring Boot 3 单体 + MySQL 8。

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
│   ├── init.sql             # 唯一建表真源（11 张表 + 分类/标签/示例草稿题 seed）
│   ├── seed-demo.sql        # 演示数据（18 道发布题 + admin/demo 账号 + 自评/错题/收藏）
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
# 1) 建库 + 11 张表 + seed（root 执行；--default-character-set 保证 seed 中文不乱码）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/init.sql"

# 2) 建应用账号并授权（root 执行；db/grant.sql 已被 gitignore）
Copy-Item db\grant.sql.example db\grant.sql
#    编辑 db/grant.sql，在两个 IDENTIFIED BY '' 里填入应用账号密码（本机开发为 123456）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/grant.sql"
```

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
& "D:\Develop\apache-maven-3.9.16\bin\mvn.cmd" -q test                        # 单测（AI 上游走 mock，不需要真实 Key）

# 前端
cd frontend
& "D:\Develop\nodejs\npm.cmd" run lint
& "D:\Develop\nodejs\npm.cmd" run build
```

## 7. 部署（单机 Nginx + 单 jar + 本机 MySQL）

> V1 部署形态：**Nginx 托管 `frontend/dist` + 反代 `/api` 到后端 + 本机 MySQL 单实例**。
> 部署后**仅站长一人访问**（本机或局域网 HTTP），**不做 HTTPS / 域名 / 备案 / 公网开放**（V1.5 才做）。
> 参考 `nginx/default.conf`、`backend/Dockerfile`、`.env.example`、`scripts/backup.sh`。

### 7.1 一次性准备

```powershell
# 1) 建库 + 11 张表 + seed（root 执行）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/init.sql"

# 2) 演示数据（18 道发布题 + admin/demo 账号 + 自评/错题/收藏；在 init.sql 之后执行一次）
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/seed-demo.sql"

# 3) 应用账号与授权（db/grant.sql 已被 gitignore）
Copy-Item db\grant.sql.example db\grant.sql   # 填密码后：
& "D:\Develop\MySQL\MySQLServer8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 `
  -e "source D:/Develop/Agent/projects/CodeInterview/db/grant.sql"
```

### 7.2 构建产物

```powershell
# 前端：产物在 frontend/dist
cd frontend; & "D:\Develop\nodejs\npm.cmd" ci; & "D:\Develop\nodejs\npm.cmd" run build

# 后端：产物是 backend/target/codeinterview-backend-1.0.0.jar
cd backend; & "D:\Develop\apache-maven-3.9.16\bin\mvn.cmd" -q clean package -DskipTests
```

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
# 方式 A：直接跑 jar（工作目录放 backend，便于读取 config/application-local.yml）
cd backend
java -Xms256m -Xmx512m -jar target/codeinterview-backend-1.0.0.jar

# 方式 B：Docker（前端仍由 Nginx 托管）
cd backend
docker build -t codeinterview-backend:1.0.0 .
docker run -d --name codeinterview-backend --env-file ../.env -p 8080:8080 codeinterview-backend:1.0.0
```

生产环境用**环境变量**提供凭据（`.env.example` 为模板，复制为 `.env` 且不入库）：
`DB_HOST/DB_PORT/DB_USERNAME/DB_PASSWORD`、`ADMIN_INIT_PASSWORD`、`AI_*`、`SPRING_PROFILES_ACTIVE=prod`。
环境变量优先级最高，会覆盖 jar 内 `application.yml`；服务器上不需要 `application-local.yml`。

> **AI 助手**：不配置 `AI_API_KEY` 时 `ai.enabled` 仍为 `true`，但 `/api/v1/ai/chat` 返回 50201（其余功能正常）。
> 本机演示可用 `--spring.profiles.active=mock`（内置 mock 上游，不需要真实 Key）。

### 7.5 部署后验证

```powershell
curl.exe http://127.0.0.1/api/v1/health                 # {"status":"UP","db":"UP",...}
# 浏览器打开 http://127.0.0.1/ → 首页应看到 ≥15 道题（演示数据），点带代码的题有高亮
# 登录 admin / 123456（演示数据密码）→ 管理端入口可用；刷题记录页有指标卡与两张图
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
| 启动报 `Access denied for user 'codeinterview'` | 应用账号未创建或密码不符：按 3.1 用 root 重建账号并 `FLUSH PRIVILEGES` |
| 分类/标签/题目 seed 中文乱码 | `mysql` 客户端字符集不对：执行时加 `--default-character-set=utf8mb4`，删库重建后重跑 `db/init.sql` |
| 8080 端口被占用 | 先停掉旧进程：`Get-NetTCPConnection -LocalPort 8080` 找到 PID 后 `Stop-Process` |
| `mvn` 不是内部或外部命令 | Maven 未加入 PATH，用全路径 `D:\Develop\apache-maven-3.9.16\bin\mvn.cmd` |
| `npm ci` 报 engines 不支持 | `node -v` 不是 v24：按第 2 节处理 PATH，或用 `D:\Develop\nodejs\npm.cmd` 绝对路径 |
| 访问不存在路径返回 404 | 正常行为：`{"code":40400,"message":"内容不存在或已下架","data":null}`（HTTP 404，由全局异常处理） |
| 想用容器 MySQL | `cd docker && docker compose up -d mysql`，容器映射 3307；应用侧设 `DB_PORT=3307`，默认不要启用 |
| 前端刷新非首页路由 404（部署） | Nginx 必须配 `try_files $uri $uri/ /index.html`，见 `nginx/default.conf` |

## 9. 安全与配置约定（交付前检查）

- `backend/config/application-local.yml`、`db/grant.sql`、`.env` 均被 `.gitignore` 忽略，**不得提交**；
  模板文件 `application-local.yml.example` 的密钥字段必须为空。
- 日志、接口响应、前端代码中不得出现密码、token、AI Key（讯飞 Key 前缀是 `ak-`）。
- 统一返回 `Result`（`/api/v1/health` 唯一例外），业务错误 HTTP 状态同步，Long 类型 id 出参为字符串。
- 数据库账号只授权 `codeinterview.*`，应用不以 root 连接。
