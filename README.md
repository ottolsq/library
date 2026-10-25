# 藏書閣图书管理系统

基于 Spring Boot 3 + Vue 3 的图书管理系统，Web 实训课程项目。系统面向管理员与读者两类角色，覆盖读者自助注册、图书管理、读者档案、借阅流转、逾期识别和权限控制等核心流程。

## 功能概览

- **认证与账号**：读者自助注册、账号密码登录、退出登录、本人修改密码、JWT + Redis 登录态。
- **权限控制**：管理员/读者两类角色；菜单、路由、按钮三级控制；后端路径级与方法级双重鉴权。
- **用户管理**：系统账号新增、编辑、启用/禁用、重置密码、删除。
- **读者档案**：姓名/学号/电话检索，新增、编辑、删除，关联系统账号与可借上限。
- **图书管理**：书名/作者/ISBN 关键词与分类组合查询，新增、编辑、逻辑删除、库存查看。
- **借阅管理**：借书、还书、状态筛选、逾期自动标记与逾期天数、读者数据隔离、归属校验。
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
│   └── 03_permission_alignment.sql  # 读者权限对齐
├── docs/                    # 本地文档（默认被 .gitignore 忽略）
│   ├── report/              # 作业报告 docx
│   ├── figures/             # 报告插图
│   ├── test-records/        # 测试证据
│   └── frontend/            # 前端设计、截图、线框
└── logs/                    # 运行日志（默认忽略）
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

### 2. 配置后端

开发环境配置位于 `library-admin/src/main/resources/application-dev.yml`，按本机情况修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/book_library?...
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
```

生产配置请复制 `application-prod.yml.example` 为 `application-prod.yml`，并通过环境变量注入真实值。`application-prod.yml` 已被 `.gitignore` 忽略。

### 3. 启动后端

```bash
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
| `application.yml` | 公共配置 | 应用名、默认端口、Redis、Druid、MyBatis-Plus、CORS 默认值、JWT 密钥占位符 |
| `application-dev.yml` | 开发环境 | 本地 `book_library`、SQL 日志、Druid 监控页（仅本机） |
| `application-test.yml` | 测试环境 | `library_test` 独立库、端口 8081 |
| `application-prod.yml` | 生产环境 | 端口 8082、数据库/CORS 环境变量；文件不入库 |
| `application-prod.yml.example` | 生产配置模板 | 只含占位符 |

常用环境变量：

```text
DB_URL / DB_USERNAME / DB_PASSWORD
JWT_SECRET
REDIS_HOST / REDIS_PORT
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
| `/borrow` | POST | `borrow:add` |
| `/borrow/{id}/return` | PUT | `borrow:edit`，非管理员校验记录归属 |
| `/borrow/{id}` | DELETE | `borrow:remove`，仅已归还记录 |
| `/menu/tree` | GET | 已登录，按角色返回菜单 |

## 测试

```bash
# 后端单元测试
mvn test

# 前端类型检查与构建
cd library-ui && npm run build
```

当前后端测试覆盖统一响应、JWT、借阅规则、本人改密和读者注册，共 23 个用例。测试证据、JMeter 结果、Playwright 兼容性和性能数据保存在 `docs/test-records/`（本地目录，默认不入库）。

## 安全说明

- 密码使用 BCrypt 哈希存储，数据库不保存明文。
- JWT 载荷只包含用户 ID、签发时间和过期时间；登录态与权限保存在 Redis。
- 退出登录、修改密码会删除 Redis 登录态；改密记录毫秒级时间，旧令牌即使重新登录也无法复用。
- 权限采用菜单、路由、按钮三级控制，后端通过路径级 `PERMISSION_RULES` 与方法级 `@PreAuthorize` 双重校验。
- 借阅查询按当前用户读者档案过滤；归还/删除再次校验记录归属，防止水平越权。
- MyBatis-Plus 参数绑定与 Druid `wall` 过滤器降低 SQL 注入风险；全局异常处理不向前端返回堆栈。
- 数据库/JWT 等敏感配置通过环境变量注入，生产配置文件不入库。

## 已知限制与后续计划

- 未实现连续登录失败锁定和密码复杂度校验，当前密码策略仅限制 6～64 位。
- 统计报表（F17）和操作日志（F18）尚未实现。
- 多校区通借通还、线上续借/预约（F19/F20）不在本次范围。
- 角色/权限修改后，已登录用户需要重新登录才能刷新 Redis 中的权限缓存。
- 生产环境 Nginx + JAR 的实际部署需在目标服务器上完成验证。

## 文档

- `docs/report/`：作业报告与课程材料
- `docs/figures/`：页面跳转图、部署结构图
- `docs/frontend/`：前端设计草图、界面截图、线框图
- `docs/test-records/`：功能测试记录、性能数据、兼容性结果、JMeter 原始数据

> `docs/`、`logs/` 和本地 IDE 文件默认被 `.gitignore` 忽略，仅本地保留；如需要随代码提交文档，可调整 `.gitignore`。
