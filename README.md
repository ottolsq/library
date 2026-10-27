# 藏書閣图书管理系统

基于 Spring Boot 3 + Vue 3 的图书管理系统，Web 实训课程项目。系统面向管理员与读者两类角色，覆盖读者自助注册、图书管理、读者档案、借阅流转、逾期识别和权限控制等核心流程。

## 功能概览

- **认证与账号**：读者自助注册、账号密码登录、退出登录、本人修改密码、JWT + Redis 登录态。
- **权限控制**：管理员/读者两类角色；菜单、路由、按钮三级控制；后端路径级与方法级双重鉴权。
- **用户管理**：系统账号新增、编辑、启用/禁用、重置密码、删除。
- **读者档案**：姓名/学号/电话检索，新增、编辑、删除，关联系统账号与可借上限。
- **图书管理**：书名/作者/ISBN 关键词与分类组合查询，新增、编辑、逻辑删除、库存查看。
- **借阅管理**：管理员可为任意读者借书/还书；读者可自助借书（只能为本人办理），还书仍由管理员办理；状态筛选、逾期自动标记与逾期天数、读者数据隔离、归属校验。
- **公共能力**：统一响应 `{code,message,data}`、错误码枚举、全局异常处理、参数校验、Druid 连接池、多环境配置。

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Java 21、Spring Boot 3.3.4、Spring Security、JWT（Hutool）、MyBatis-Plus 3.5.7、Druid、MySQL 8、Redis |
| 前端 | Vue 3.5、Vite 8、TypeScript、Element Plus、Pinia、Vue Router、Axios |
| 测试 | JUnit 5、Mockito、Playwright、curl/Python、JMeter |
| 构建 | Maven 多模块、npm |

## 模块结构

| 模块 | 职责 |
|---|---|
| `library-common` | 统一响应、错误码、业务异常、全局异常处理、JWT 工具、安全常量、跨模块接口 |
| `library-web` | Spring Security 配置、JWT 认证过滤器、登录服务、Redis 缓存、认证/授权异常处理 |
| `library-admin` | 启动类与业务模块：用户、读者、图书、借阅、菜单、注册、定时任务、DTO/Mapper |
| `library-ui` | Vue 3 前端：登录/注册、主布局、用户/读者/图书/借阅页面 |

`library-admin` 依赖 `library-web` 与 `library-common`，是唯一可启动的 Spring Boot 模块。

## 目录结构

```text
library/
├── README.md
├── pom.xml
├── .gitignore
├── library-common/          # 公共模块
├── library-web/             # 认证与安全模块
├── library-admin/           # 业务与启动模块
├── library-ui/              # Vue 3 前端
├── sql/
│   ├── 01_schema.sql        # 库与基础表
│   ├── 02_menu.sql          # 菜单、角色绑定、种子数据
│   ├── 03_permission_alignment.sql  # 读者自助借书权限对齐
│   └── 04_unique_key_cleanup.sql    # 补齐旧库 is_deleted 并清理唯一键
```

## 环境要求

- JDK 21（Maven 编译目标为 21）
- Maven 3.9+
- MySQL 8.0+
- Redis 5.0+
- Node.js 20+、npm 10+

## 快速开始

### 1. 初始化数据库

```bash
# 01 会删除并重建 book_library，执行前确认无需保留数据
mysql -uroot -p < sql/01_schema.sql
mysql -uroot -p book_library < sql/02_menu.sql
mysql -uroot -p book_library < sql/03_permission_alignment.sql
```

`03_permission_alignment.sql` 授予读者 `borrow:add`（自助借书），同时收回 `borrow:edit`（还书仍由管理员办理）。执行权限 SQL 后已登录用户需重新登录。

如果沿用旧数据库，再执行 `sql/04_unique_key_cleanup.sql`：它会补齐旧 `user`/`book` 表缺失的 `is_deleted` 列，并释放被已删除账号/图书占用的用户名与 ISBN。脚本可重复执行。

### 2. 配置后端

`application-dev.yml` / `application-test.yml` **不保存任何明文密码**，数据库密码、Druid 监控凭据、JWT 密钥都通过环境变量注入。最少需要设置：

```text
DB_PASSWORD   数据库密码（必填）
JWT_SECRET    JWT 签名密钥（必填，建议 32 位以上随机字符串）
```

可参考 [.env.example](.env.example) 复制一份为 `.env` 填入真实值。dev 环境会通过 `application-dev.yml` 的 `spring.config.import` 自动加载项目根目录或模块上级目录的 `.env`；生产/测试环境仍请在 IDE Run Configuration、shell 或容器环境中设置变量。

Windows PowerShell 示例：

```powershell
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="请替换为至少32位的随机字符串"
```

需要 Druid 监控页时再设置：

```text
DRUID_STAT_ENABLED=true
DRUID_USERNAME=你的监控账号
DRUID_PASSWORD=你的监控密码
```

生产配置请复制 `application-prod.yml.example` 为 `application-prod.yml`，并通过环境变量注入真实值。`application-prod.yml` 已被 `.gitignore` 忽略。

### 3. 启动后端

```powershell
# 先设置 DB_PASSWORD、JWT_SECRET（见上一步）
mvn clean package -DskipTests
java -jar library-admin/target/library-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

默认端口 8080。也可以使用：

```bash
mvn -pl library-admin -am spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 启动前端

```bash
cd library-ui
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。Vite 开发服务器会把 `/api` 代理到 `http://127.0.0.1:8080`。生产构建：

```bash
npm run build
npm run preview -- --port 4173
```

### 5. 测试账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `123456` | 管理员 | 全部菜单与操作权限 |
| `reader01` | `123456` | 读者 | 读者一号，带读者档案 |
| `reader` | `123456` | 读者 | 测试读者，带读者档案 |

以上仅为本地测试账号，实际使用请修改密码或删除种子账号。读者也可以通过 `/register` 自助注册，注册后角色固定为读者，并自动创建读者档案。

## 多环境配置

| 文件 | 用途 | 关键项 |
|---|---|---|
| `application.yml` | 公共配置 | 应用名、默认端口、Redis、Druid、MyBatis-Plus、CORS 默认值、JWT 密钥（必填环境变量） |
| `application-dev.yml` | 开发环境 | 本地 `book_library`、SQL 日志、Druid 监控（默认关闭，凭据走环境变量） |
| `application-test.yml` | 测试环境 | `library_test` 独立库、端口 8081、数据库密码走环境变量 |
| `application-prod.yml` | 生产环境 | 端口 8082、数据库/CORS 环境变量；文件不入库 |
| `application-prod.yml.example` | 生产配置模板 | 只含占位符 |
| `.env.example` | 环境变量示例 | 本地/部署时需要设置的全部变量，不含真实值 |

常用环境变量：

```text
DB_URL / DB_USERNAME / DB_PASSWORD
JWT_SECRET
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD
DRUID_STAT_ENABLED / DRUID_USERNAME / DRUID_PASSWORD
CORS_ALLOWED_ORIGINS
```

环境切换通过 Maven Profiles 完成：

```bash
mvn clean package -Pprod
```

## 接口与权限

| 接口 | 方法 | 权限/说明 |
|---|---|---|
| `/user/login` | POST | 公开 |
| `/user/register` | POST | 公开，只创建读者角色 |
| `/user/current` | GET | 已登录 |
| `/user/logout` | POST | 已登录 |
| `/user/password` | PUT | 已登录，校验原密码 |
| `/user/list` | GET | `user:list` |
| `/user/{id}` | GET/PUT/DELETE | `user:query` / `user:edit` / `user:delete` |
| `/user/{id}/status` | PUT | `user:status` |
| `/user/{id}/password` | PUT | `user:resetPwd` |
| `/reader/list`、`/reader/{id}` | GET | `reader:query` |
| `/reader`、`/reader/{id}` | POST/PUT/DELETE | `reader:add` / `reader:edit` / `reader:remove` |
| `/book/list`、`/book/{id}` | GET | `book:query` |
| `/book`、`/book/{id}` | POST/PUT/DELETE | `book:add` / `book:edit` / `book:remove` |
| `/borrow/list` | GET | `borrow:query`，读者仅返回本人记录 |
| `/borrow` | POST | `borrow:add`；管理员选择读者，读者只能为本人借书 |
| `/borrow/{id}/return` | PUT | `borrow:edit`，非管理员校验记录归属；当前读者角色未授予该权限 |
| `/borrow/{id}` | DELETE | `borrow:remove`，仅已归还记录 |
| `/menu/tree` | GET | 已登录，按角色返回菜单 |

## 测试

```powershell
# 后端测试：上下文测试需要本机 MySQL/Redis，并设置数据库密码
$env:DB_PASSWORD="你的数据库密码"
mvn test

# 前端类型检查与构建
cd library-ui && npm run build
```

当前后端测试覆盖统一响应、JWT、路径权限解析、借阅规则、读者自助借书归属、图书库存校验、本人改密、读者注册、用户管理自我保护与登录态失效，共 46 个用例（45 个纯单元测试 + 1 个 Spring 上下文加载测试；上下文测试需要本机 MySQL/Redis）。测试证据、JMeter 结果、Playwright 兼容性和性能数据保存在 `docs/test-records/`（本地目录，默认不入库）。

## 安全说明

- 密码使用 BCrypt 哈希存储，数据库不保存明文。
- JWT 载荷只包含用户 ID、签发时间和过期时间；登录态与权限保存在 Redis。
- 退出登录、修改密码会删除 Redis 登录态；改密记录毫秒级时间，旧令牌即使重新登录也无法复用。
- 禁用、删除、改角色、改用户名、管理员重置密码后，在数据库事务提交后清理 Redis 登录态，旧令牌无法继续访问。
- 禁止禁用、删除、降级当前登录账号；禁止操作最后一个启用的管理员账号，最后管理员校验使用 `FOR UPDATE` 锁避免并发绕过。
- 权限采用菜单、路由、按钮三级控制，后端通过路径级 `PERMISSION_RULES` 与方法级 `@PreAuthorize` 双重校验；路径权限按“精确匹配优先”解析，避免 `/user/list` 被 `/user/{id}` 误匹配。
- 借阅查询按当前用户读者档案过滤；归还/删除再次校验记录归属，防止水平越权；读者自助借书时服务端强制使用本人读者档案。
- 读者档案只能关联启用状态的读者角色账号，且一个账号只能关联一个读者档案；编辑档案时若关联账号未变，不会因账号后来被提升为管理员而无法保存。
- 删除用户时改写用户名为唯一墓碑名，释放用户名唯一索引；关联读者存在借阅记录时拒绝删除账号。
- 删除图书时先检查借阅记录；无借阅记录时改写 ISBN 为墓碑值并逻辑删除，释放 ISBN 唯一索引。
- 图书新增/修改在后端校验可借库存不超过总册数，且编辑后的库存差额不能小于当前借出数量。
- MyBatis-Plus 参数绑定与 Druid `wall` 过滤器降低 SQL 注入风险；全局异常处理不向前端返回堆栈。
- 数据库密码、Druid 监控凭据、JWT 密钥等敏感配置全部通过环境变量注入，配置文件中不保存明文；`JWT_SECRET` 未配置时应用直接启动失败。

## 已知限制与后续计划

- 未实现连续登录失败锁定和密码复杂度校验，当前密码策略仅限制 6～64 位。
- 统计报表（F17）和操作日志（F18）尚未实现。
- 多校区通借通还、线上续借/预约（F19/F20）不在本次范围。
- 角色/权限修改后会话会被清理，用户需要重新登录；执行更新权限的 SQL 后同样需要重新登录。
- 读者当前只能自助借书，不能自助还书或删除借阅记录，还书仍需管理员办理。
- 生产环境 Nginx + JAR 的实际部署需在目标服务器上完成验证。

## 文档

- `docs/report/`：作业报告与课程材料
- `docs/figures/`：页面跳转图、部署结构图
- `docs/frontend/`：前端设计草图、界面截图、线框图
- `docs/test-records/`：功能测试记录、性能数据、兼容性结果、JMeter 原始数据

> `docs/`、`logs/` 和本地 IDE 文件默认被 `.gitignore` 忽略，仅本地保留；如需要随代码提交文档，可调整 `.gitignore`。
