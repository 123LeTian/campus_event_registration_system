# 校园活动报名系统

## 项目简介

校园活动报名系统面向校园讲座、社团活动、竞赛、志愿活动等场景，已实现活动发布、学生报名、名额控制、报名名单、我的报名、统计可视化和 AI 辅助活动内容生成等完整业务闭环。

## 当前阶段

当前为 **Phase 14 最终验收与交付整理**。Phase 1 到 Phase 12 的核心开发、统一异常安全、前端 UI 优化和完整测试已经完成；本阶段聚焦 README、测试报告和最终验收报告整理，便于 5 到 10 分钟课程验收演示。

AI 生成结果不会直接写入数据库。活动介绍必须由组织者审核、修改并保存活动后，才会写入 `event.description`；宣传文案仅用于页面展示和复制，不新增数据库字段、不入库。本项目不实现导出 Excel、签到、二维码、报名审核、站内消息通知或新的 AI 功能。Phase 12 测试结果见 [测试报告](docs/test-report.md)，Phase 14 交付整理见 [最终验收报告](docs/final-acceptance-report.md)，Phase 4 交付和验证结果见 [产品经理报告](docs/Phase4-产品经理交付报告.md)。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.5、Spring Security 6、Spring Validation、MyBatis-Plus、JJWT、Maven
- 前端：Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus、ECharts
- 数据库：MySQL 8
- 包名：`com.example.campusevent`

## 环境变量

后端数据库和 JWT 配置优先从环境变量读取，默认值仅用于本地开发占位，不要用于生产环境。

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/campus_event?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false` | MySQL 连接地址 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | 空 | MySQL 密码 |
| `JWT_SECRET` | `local-development-secret-change-me-local-development-secret-change-me` | 本地开发 JWT 密钥占位值，正式演示或生产必须改为高强度随机值 |
| `JWT_EXPIRATION_MS` | `7200000` | JWT 过期时间，默认 2 小时 |
| `AI_API_KEY` | 空 | 大模型服务密钥，不要提交到 Git；正式 Demo 必须配置 |
| `AI_MODEL` | `gpt-4o-mini` | 大模型名称 |
| `AI_BASE_URL` | `https://api.openai.com/v1/chat/completions` | OpenAI-compatible Chat Completions 地址 |
| `AI_MOCK_ENABLED` | `false` | 是否启用 Mock 模式；仅用于自动化测试或无 Key 开发环境 |
| `AI_TIMEOUT_MS` | `15000` | AI 调用超时时间，单位毫秒 |

AI Key 只从环境变量或本地未提交配置读取，不会出现在前端代码、README 示例或测试输出中。数据库密码、JWT Secret 和 AI Key 不要写入仓库，不要提交 `.env`、真实密码、真实密钥或本地私有配置。正式演示应设置 `AI_MOCK_ENABLED=false` 并配置真实 `AI_API_KEY`。

## 后端认证模块结构

```text
backend/src/main/java/com/example/campusevent/
├── common/api/ApiResponse.java, ErrorCode.java
├── common/exception/BusinessException.java, GlobalExceptionHandler.java
├── common/security/AuthenticatedUser.java, JwtAuthenticationFilter.java, JwtTokenProvider.java, SecurityConfig.java, SecurityProbeController.java
├── common/util/CurrentUserUtils.java
└── user/
    ├── controller/AuthController.java, UserController.java
    ├── dto/LoginRequest.java, RegisterRequest.java
    ├── entity/User.java, UserRole.java
    ├── mapper/UserMapper.java
    ├── service/UserService.java, UserServiceImpl.java
    └── vo/LoginVO.java, UserVO.java
```

## 已实现 API

### 健康检查

```text
GET /api/health
```

匿名允许访问，返回后端运行状态。

### 用户注册

```text
POST /api/auth/register
```

请求示例：

```json
{
  "username": "newStudent",
  "password": "Student123456",
  "name": "新学生"
}
```

普通注册用户固定创建为 `STUDENT`，即使请求体额外提交 `role: "ADMIN"` 也会被后端忽略。注册成功返回 `UserVO`，不返回密码。

### 用户登录

```text
POST /api/auth/login
```

请求示例：

```json
{
  "username": "organizer1",
  "password": "Org123456"
}
```

登录成功返回 JWT 和用户信息：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "JWT_TOKEN",
    "user": {
      "id": 2,
      "username": "organizer1",
      "name": "活动组织者",
      "role": "ORGANIZER"
    }
  }
}
```

### 当前用户

```text
GET /api/users/me
Authorization: Bearer <token>
```

必须携带有效 JWT，返回当前登录用户，不包含密码。Token 缺失、非法或过期时返回 401。

### 权限验证探针

```text
GET /api/security/admin-check
Authorization: Bearer <token>
```

该接口仅用于 Phase 3 测试 401/403 和 `ADMIN` 角色授权，不属于活动业务模块。未登录返回 401，非管理员返回 403，管理员返回成功。

## JWT、BCrypt 和角色权限

- 密码通过 `BCryptPasswordEncoder` 加密后写入 `user.password`，不保存明文。
- JWT 使用 JJWT 0.12.x 签发，包含 `userId`、`username`、`role`、签发时间和过期时间。
- JWT 密钥从 `JWT_SECRET` 或本地开发占位配置读取，不在代码中写入真实密钥。
- 使用本地默认 `JWT_SECRET` 启动时后端会输出 WARN 日志；正式演示或生产环境必须配置强随机密钥。
- `JwtAuthenticationFilter` 支持 `Authorization: Bearer <token>`，校验失败直接返回统一 401 响应。
- 认证成功后将 `AuthenticatedUser` 写入 Spring Security `SecurityContext`。
- 角色权限使用 `ROLE_STUDENT`、`ROLE_ORGANIZER`、`ROLE_ADMIN`。
- 已启用 `@EnableMethodSecurity`，后续业务模块可使用 `@PreAuthorize`。

## 统一错误码和异常响应

所有业务 API 使用统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误码统一如下：

| code | HTTP 状态 | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40000 | 400 | 参数错误、请求体格式错误、参数类型错误 |
| 40100 | 401 | 未认证、Token 缺失、Token 非法、Token 过期 |
| 40300 | 403 | 无权限、角色不足、访问他人资源 |
| 40400 | 404 | 资源不存在、活动不存在或已删除 |
| 40900 | 409 | 业务冲突，例如重复报名、满员、已截止、已结束、没有有效报名、数据库唯一约束冲突 |
| 50000 | 500/502/503/504 | 系统异常、数据库访问异常、AI 配置缺失、AI 超时或 AI 服务不可用 |

`GlobalExceptionHandler` 已统一处理参数校验、参数绑定、缺失参数、参数类型错误、请求体格式错误、认证异常、权限异常、业务异常、数据库唯一约束冲突、数据库访问异常、404/方法不支持和兜底异常。

参数错误尽量返回具体字段提示；数据库约束冲突只返回可读业务提示，不暴露 SQL、索引名或数据库细节；兜底异常记录 ERROR 日志和 request path，但响应中不包含 stack trace。

## 日志与安全加固

后端使用 Spring Boot 默认 Logback，不引入额外日志平台。关键日志包括：

- 登录成功：INFO，记录 `userId`、`username`、`role`。
- 登录失败：WARN，记录 `username` 和失败原因，不记录密码。
- 注册成功：INFO，记录 `userId`、`username`。
- 活动创建、编辑：INFO，记录活动和操作者关键信息。
- 活动删除：WARN，记录 `eventId`、`operatorId`。
- 报名成功、取消报名：INFO，记录 `eventId`、`userId`。
- AI 配置缺失、超时、服务不可用：WARN，不记录 API Key。
- 越权访问：WARN，记录用户、路径或动作。
- 数据库访问异常和未预期异常：ERROR，记录 path 和 stack trace，前端只收到统一错误文案。

安全边界保持后端兜底：注册接口强制 `STUDENT`；`STUDENT` 不能管理活动、查看名单、访问统计或 AI 接口；`ORGANIZER` 只能管理和统计自己的活动；`ADMIN` 按设计拥有全局管理和统计权限。MyBatis 查询使用 `#{}` 参数绑定处理用户输入，keyword 搜索不会拼接原始 SQL；报名名单 SQL 中的 `${columns}` 只接收服务端固定常量，不接收用户输入。

前端 `src/api/request.ts` 统一处理错误：401 清理登录态并跳转登录页；403 显示无权限；400/409 展示后端 message；5xx 显示“系统繁忙，请稍后重试”；网络异常提示后端服务检查；短时间重复错误会抑制重复弹窗。前端不在 console 输出 Token。

## 前端认证实现

- `src/views/LoginView.vue`：登录表单、基础校验、调用登录 API、成功后保存 Token 和用户信息。
- `src/views/RegisterView.vue`：注册表单、确认密码校验、无角色选择，成功后跳转登录页。
- `src/stores/auth.ts`：Pinia 保存 Token 和当前用户，支持刷新当前用户和退出登录。
- `src/api/request.ts`：Axios 自动添加 `Authorization: Bearer <token>`，统一处理 401、403、409、网络异常和后端错误消息。
- `src/router/index.ts`：`/login`、`/register` 匿名访问，`/` 需要登录。
- `src/views/HomeView.vue`：展示当前登录用户名、角色和退出登录按钮。

## 数据库初始化

数据库名称为 `campus_event`，SQL 文件使用 MySQL 8.x 语法、`utf8mb4` 字符集和 InnoDB 引擎。`database/schema.sql` 和 `database/seed.sql` 已包含 `user`、`event`、`registration` 三张表及 Demo 用户。

执行建表脚本：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

在 mysql 提示符中执行建表和 Demo 用户脚本：

```sql
SET NAMES utf8mb4;
source D:/Project/campus_event_registration_system/database/schema.sql;
source D:/Project/campus_event_registration_system/database/seed.sql;
```

以上 source 命令在 mysql 提示符下执行，路径按实际目录调整。Windows 客户端默认 GBK 可能导致 UTF-8 中文名称导入失败（ERROR 1406），请使用 utf8mb4。schema.sql 使用 IF NOT EXISTS，不会自动迁移已有表。

Demo 账号如下：

| 用户名 | Demo 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin123456` | `ADMIN` |
| `organizer1` | `Org123456` | `ORGANIZER` |
| `studentA` | `Stu123456` | `STUDENT` |
| `studentB` | `Stu123456` | `STUDENT` |
| `studentC` | `Stu123456` | `STUDENT` |
| `studentD` | `Stu123456` | `STUDENT` |

Demo 密码只用于本地验收说明，数据库 `user.password` 字段只保存 BCrypt 哈希，不保存明文密码。

## 启动方式

后端：

```bash
cd backend
mvn spring-boot:run
```

如果系统 PATH 未配置 Maven，本机可使用：

```powershell
D:\DevEnv\Maven\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

前端：

```bash
cd frontend
pnpm install
pnpm dev
```

默认访问地址：

```text
http://localhost:5173
```

## 验收方式

后端测试：

```bash
cd backend
mvn test
```

前端构建：

```bash
cd frontend
pnpm build
```

推荐手工验收流程：

1. 初始化 MySQL：执行 `database/schema.sql` 和 `database/seed.sql`。
2. 启动后端和前端。
3. 使用 `organizer1 / Org123456` 登录成功。
4. 使用错误密码登录失败并显示后端错误。
5. 注册一个新用户，确认返回角色为 `STUDENT`。
6. 使用新注册用户登录。
7. 携带 Token 请求 `GET /api/users/me` 成功。
8. 删除或篡改 Token 后访问 `/api/users/me` 返回 401。
9. 使用学生 Token 访问 `/api/security/admin-check` 返回 403。
10. 点击退出登录后 Token 和用户信息被清除，受保护首页需要重新登录。

## 已完成测试覆盖

- 注册成功。
- 注册用户名重复。
- 注册密码 BCrypt 加密。
- 注册请求携带 `role=ADMIN` 时最终仍为 `STUDENT`。
- 登录成功并返回 JWT。
- 登录密码错误。
- 禁用用户不能登录。
- `/api/users/me` 携带有效 Token 可以访问。
- `/api/users/me` 不携带 Token 返回 401。
- 非法 Token 返回 401。
- 过期 Token 返回 401。
- `STUDENT`、`ORGANIZER`、`ADMIN` 角色解析正确。
- 受保护接口区分未登录 401 和角色不足 403。
- `ORGANIZER`、`ADMIN` 可调用 AI 活动介绍生成。
- `STUDENT` 调用 AI 活动介绍生成返回 403。
- 未登录调用 AI 活动介绍生成返回 401。
- AI 请求缺少标题或地点返回 400。
- Mock 模式下活动介绍返回 introduction、highlights、audience。
- Mock 模式下宣传文案返回 promotion。
- AI Key 缺失且未开启 Mock 时返回明确业务错误。
- AI 超时转换为明确业务错误。
- AI 返回格式异常转换为明确业务错误。
- AI 接口不写入 event 表。
- AI 活动介绍经人工确认后可保存为 event.description。
- `ORGANIZER` 获取统计概览时只统计自己活动。
- `ADMIN` 获取统计概览时统计全部未删除活动。
- `STUDENT` 访问统计接口返回 403。
- 未登录访问统计接口返回 401。
- 统计总活动数排除 `deleted=1` 活动。
- 统计报名总人数只统计 `registration.status='ACTIVE'`。
- 报名中、已结束、已满员、报名截止状态分布按后端 displayStatus 规则计算。
- 活动统计列表返回剩余名额、报名率、分页 total，并按角色隔离统计范围。

## Phase 4 活动管理

后端位于 `backend/src/main/java/com/example/campusevent/event/`，采用 controller、service、mapper、entity、dto、vo 分层。组织者名称关联 user 查询，不新增数据库字段。

| API | 用途 | 权限 |
| --- | --- | --- |
| GET /api/events | 基础分页、keyword 查询；mine=true 按当前用户过滤 | 已登录 |
| GET /api/events/{id} | 未删除活动详情 | 已登录 |
| POST /api/events | 创建并发布活动 | ORGANIZER、ADMIN |
| PUT /api/events/{id} | 编辑活动 | 创建者 ORGANIZER 或 ADMIN |
| DELETE /api/events/{id} | deleted=1 软删除 | 创建者 ORGANIZER 或 ADMIN |

分页默认 page=1、pageSize=10，pageSize 上限 50。关键词在 SQL 中筛选，total 使用相同条件。组织者管理页传 mine=true；管理员管理页查询全部未删除活动。大厅供所有登录角色查看全部未删除活动。

活动结束后禁止编辑；已有报名时锁定三个时间字段；人数上限不能小于当前人数。删除保留活动与报名历史，所有角色访问已删除详情均返回 404。

displayStatus 依次判断 DELETED、FINISHED、CLOSED、FULL、REGISTRATION_OPEN，前端只展示后端返回值。剩余名额为 maxParticipants-currentParticipants；报名率为 currentParticipants/maxParticipants*100，四舍五入保留两位。活动详情会为 STUDENT 准确返回 `registeredByCurrentUser`，活动列表保持轻量返回 false，避免为每条活动额外查询报名状态。

前端路由：`/events`、`/events/:id`、`/manage/events`、`/manage/events/new`、`/manage/events/:id/edit`。学生管理入口隐藏，管理路由拦截，后端独立校验权限。首页保留当前用户和退出按钮。

浏览器回归脚本 `docs/verification/phase4-browser.cjs` 使用 Playwright 和本机 Edge，调用运行中的 API，不模拟业务响应。默认连接 5173 前端和 8080 后端。可通过 PHASE4_PLAYWRIGHT 指定 Playwright 包路径，PHASE4_API 指定后端，PHASE4_RESULTS 指定结果目录；PHASE4_NATIVE_PROXY=true 使用前端原生代理，不拦截浏览器请求。结果包含检查记录和桌面、手机截图。

```powershell
node docs/verification/phase4-browser.cjs
```

## Phase 5 AI 辅助创作

后端新增 `backend/src/main/java/com/example/campusevent/ai/` 模块，包含 controller、service、client、dto、vo 分层。`AiModelClient` 使用 Java 17 `HttpClient` 调用 OpenAI-compatible Chat Completions 接口，读取 `AI_API_KEY`、`AI_MODEL`、`AI_BASE_URL`、`AI_MOCK_ENABLED`、`AI_TIMEOUT_MS` 配置。

| API | 用途 | 权限 |
| --- | --- | --- |
| POST /api/ai/event-introduction | 根据标题、地点、时间、活动类型、人群、关键词生成活动介绍、亮点和适合人群 | ORGANIZER、ADMIN |
| POST /api/ai/event-promotion | 根据活动上下文和可选介绍生成宣传文案、亮点和适合人群 | ORGANIZER、ADMIN |

`STUDENT` 调用 AI 接口返回 403，未登录调用返回 401。AI 接口不写入 `event` 表，不新增 AI 字段。AI 服务未配置时返回“AI 服务未配置，请手工填写”；超时时返回“AI 生成超时，请稍后重试或手工填写”；服务异常时返回“AI 生成暂不可用，请手工填写”；返回格式异常时提示重新生成或手工填写。

前端新增 `frontend/src/api/ai.ts`、`frontend/src/types/ai.ts`、`frontend/src/components/AiGenerateButton.vue`，并在 `EventFormView.vue` 增加“AI 辅助创作”区域。该区域只对 `ORGANIZER` 和 `ADMIN` 可见，生成前要求活动标题、地点、开始时间、结束时间已填写。生成的活动介绍先展示为草稿，点击“应用到活动介绍”后写入表单 `description`，之后仍可手工编辑；宣传文案支持复制，不进入活动保存载荷。

Mock 模式会根据当前表单上下文生成结构化测试内容，便于自动化测试和无 Key 开发。正式 Demo 应关闭 Mock 并配置真实 Key。AI 失败不会清空表单，也不会阻止组织者手工填写并保存活动。

## Phase 6 报名核心业务

后端新增 `backend/src/main/java/com/example/campusevent/registration/` 模块，包含 controller、service、mapper、entity、vo 分层。

| API | 用途 | 权限 |
| --- | --- | --- |
| POST /api/events/{id}/registrations | 当前学生报名活动，返回最新 EventVO | STUDENT |
| DELETE /api/events/{id}/registrations/me | 当前学生取消自己的有效报名，返回最新 EventVO | STUDENT |
| GET /api/events/{id} | 活动详情并返回当前学生是否已报名 | 已登录 |

报名使用 MySQL 事务、活动行锁、条件 `UPDATE` 和 `registration(event_id, user_id)` 唯一约束共同保证一致性。重复报名返回 `40900` 和“您已经报名该活动”；满员、报名截止、活动结束、已删除活动均会被后端拒绝。取消报名只允许学生取消自己的 `ACTIVE` 记录，成功后在同一事务内减少 `event.current_participants`，名额恢复后其他学生可以继续报名。取消后重新报名会恢复同一条报名记录为 `ACTIVE`，不会插入重复记录。

每次报名或取消都会校验 `event.current_participants` 与该活动 `ACTIVE` 报名记录数一致；任一步失败都会回滚，避免人数虚增或虚减。前端活动详情页对学生展示“立即报名”或“取消报名”按钮，操作成功后使用后端返回的最新活动数据刷新人数、剩余名额、报名率、状态和报名标记。






## Phase 7 报名名单与我的报名

后端继续复用 `backend/src/main/java/com/example/campusevent/registration/` 模块，新增报名名单和我的报名分页查询能力。查询结果使用项目统一 `PageResponse`，返回 `records`、`total`、`page`、`pageSize`。

| API | 用途 | 权限 |
| --- | --- | --- |
| GET /api/events/{id}/registrations | 查看指定活动报名名单，支持分页和状态筛选 | ORGANIZER、ADMIN |
| GET /api/my/registrations | 当前学生查看自己的报名记录，支持分页和状态筛选 | STUDENT |

活动报名名单权限由后端兜底：`ADMIN` 可以查看任意未删除活动；`ORGANIZER` 只能查看自己创建的未删除活动；`STUDENT` 访问返回 403；未登录返回 401；活动不存在或已删除返回 404。名单查询关联 `registration`、`event`、反引号包裹的 ``user`` 表，只返回学生账号、姓名和报名信息，不返回密码、JWT 或敏感字段。

我的报名仅允许 `STUDENT` 访问，且始终按当前登录用户 ID 查询，学生只能看到自己的记录。`ORGANIZER` 和 `ADMIN` 访问返回 403。我的报名使用 `LEFT JOIN event` 保留历史记录；如果活动已软删除，返回记录仍保留，`eventDisplayStatus` 标记为 `DELETED`，前端禁用“查看详情”按钮，避免跳转到已删除活动详情。

两个列表都支持 `page`、`pageSize` 和 `status` 参数：`page` 默认 1，`pageSize` 默认 10 且最大 50；`status` 支持 `ACTIVE`、`CANCELLED`、`ALL`，空值或未传默认 `ACTIVE`，非法状态返回 `40000`。`ALL` 会同时返回有效和已取消报名，count 查询与列表筛选条件保持一致。

`RegistrationVO` 字段包括：`id`、`eventId`、`eventTitle`、`eventLocation`、`eventStartTime`、`eventEndTime`、`eventDisplayStatus`、`userId`、`studentUsername`、`studentName`、`status`、`registrationTime`、`cancelledTime`。活动报名名单页主要展示学生姓名、学生账号、报名时间、报名状态、取消时间；我的报名页展示活动标题、地点、活动时间、报名时间、取消时间、报名状态、活动状态和查看详情入口。

前端新增 `frontend/src/views/RegistrationListView.vue` 和 `frontend/src/views/MyRegistrationsView.vue`，扩展 `frontend/src/api/registration.ts`、`frontend/src/types/registration.ts`、`frontend/src/router/index.ts`。活动管理页 `/manage/events` 增加“查看名单”入口，学生首页增加“我的报名”入口；前端路由仅做体验拦截，最终权限仍以后端校验为准。

## Phase 8 搜索、筛选与分页完善

`GET /api/events` 继续作为活动大厅和活动管理页的统一列表接口，支持 `page`、`pageSize`、`keyword`、`status`、`mine` 查询参数。`page` 小于 1 时按 1 处理；`pageSize` 默认 10，最大 50；`keyword` 会去除首尾空格，空字符串视为未传，并在 SQL 中匹配 `title`、`description`、`location`。

`status` 使用后端统一计算的 `displayStatus`，支持 `REGISTRATION_OPEN`、`FULL`、`CLOSED`、`FINISHED`、`DELETED`。displayStatus 优先级为：`deleted=1` 显示 `DELETED`；活动结束显示 `FINISHED`；报名截止显示 `CLOSED`；人数满显示 `FULL`；否则显示 `REGISTRATION_OPEN`。非法 status 返回 `40000`。

EventMapper 使用自定义 SQL 子查询先计算 displayStatus，再在外层按 status 过滤、排序和分页。records 查询与 count 查询共用同一套 `organizerId`、`keyword`、`status`、`deleted` 条件，避免先分页再筛选或 total 与列表不一致。排序按 `start_time DESC, id DESC`。

普通活动大厅默认只展示未删除活动。`DELETED` 筛选仅 `ADMIN` 可用，非管理员请求 `status=DELETED` 返回 403。`mine=true` 用于管理列表：`ORGANIZER` 只能查看自己的活动，`ADMIN` 可查看自己创建的活动或不传 mine 查看全部未删除活动，`STUDENT` 传 `mine=true` 返回 403。

前端活动大厅 `EventHallView.vue` 增加状态筛选控件：全部、报名中、已满员、报名截止、已结束；搜索和筛选都会重置到第 1 页，分页切换会保留当前 keyword 和 status。活动管理页 `ManageEventsView.vue` 同样增加状态筛选，组织者默认携带 `mine=true`，管理员可额外选择“已删除”。状态筛选全部走后端 API，前端只展示后端返回的 `displayStatus` 标签，不做本地业务过滤。

## Phase 9 统计可视化

后端新增 `backend/src/main/java/com/example/campusevent/statistics/` 模块，包含 controller、service、mapper、vo 分层。统计接口统一前缀为 `/api/statistics`，返回项目统一 `ApiResponse`，分页接口返回 `PageResponse`。

| API | 用途 | 权限 |
| --- | --- | --- |
| GET /api/statistics/overview | 统计活动总数、ACTIVE 报名总人数、报名中活动数、已结束活动数 | ORGANIZER、ADMIN |
| GET /api/statistics/events?page=1&pageSize=10 | 分页返回活动报名率、剩余名额、当前人数、最大人数和 displayStatus | ORGANIZER、ADMIN |
| GET /api/statistics/status-distribution | 返回 REGISTRATION_OPEN、FULL、CLOSED、FINISHED 四类状态分布 | ORGANIZER、ADMIN |

统计口径如下：

- 普通统计全部排除 `event.deleted=1`，不返回 `DELETED` 状态。
- 报名总人数只统计 `registration.status='ACTIVE'`。
- `ORGANIZER` 只统计当前用户创建的未删除活动，以及这些活动上的 ACTIVE 报名。
- `ADMIN` 统计全部未删除活动，以及全部相关 ACTIVE 报名。
- `STUDENT` 访问统计接口返回 403，未登录访问返回 401。
- 活动统计列表中 `currentParticipants` 以 `event.current_participants` 为展示基准，`remainingParticipants=maxParticipants-currentParticipants` 且不小于 0。
- `registrationRate=currentParticipants/maxParticipants*100`，后端 SQL 实时计算并保留两位小数。

统计模块复用 `EventMapper.DISPLAY_STATUS_CASE` 中的 displayStatus 规则：`deleted=1` 为 `DELETED`；否则活动结束优先为 `FINISHED`；报名截止为 `CLOSED`；人数达到上限为 `FULL`；其余为 `REGISTRATION_OPEN`。普通统计查询先排除 deleted 活动，因此状态分布只包含报名中、已满员、报名截止、已结束四类，并统一返回 count=0 的状态，前端展示更稳定。

前端新增 `frontend/src/api/statistics.ts`、`frontend/src/types/statistics.ts`、`frontend/src/views/StatisticsView.vue`，路由为 `/statistics`。首页仅对 `ORGANIZER` 和 `ADMIN` 显示“统计可视化”入口；学生直接访问会被前端路由守卫拦截，后端接口仍独立兜底 403。

统计页包含四个概览数字卡片、活动状态分布环形图、活动报名率柱状图和活动统计明细表。图表使用项目已有 ECharts 依赖，容器设置稳定高度；当接口数据为空时展示空状态，不渲染误导性图表；组件卸载时释放 chart 实例，窗口 resize 时自适应。

## Phase 10 统一异常、日志与安全加固

本阶段统一后端错误响应、错误码、关键日志、安全配置和前端 Axios 错误处理，不新增业务接口。

错误码约定如下：

| code | HTTP 状态 | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40000 | 400 | 参数校验、请求体格式或参数类型错误 |
| 40100 | 401 | 未认证、Token 缺失、非法或过期 |
| 40300 | 403 | 角色不足、越权访问 |
| 40400 | 404 | 资源不存在或已删除 |
| 40900 | 409 | 重复报名、满员、已截止、已结束、无有效报名或约束冲突 |
| 50000 | 500/502/503/504 | 系统异常、数据库异常或 AI 服务异常 |

`GlobalExceptionHandler` 统一处理参数校验、参数绑定、缺失参数、类型错误、请求体格式错误、认证/权限异常、业务异常、数据库唯一约束冲突、数据库访问异常、404/方法不支持和未预期异常。响应不包含 stack trace、SQL、索引名、密码或其他敏感信息。

关键业务日志覆盖登录成功/失败、注册、活动创建/编辑/删除、报名/取消报名、AI 配置或调用失败、越权访问、数据库异常和系统异常。日志不记录明文密码、完整 JWT、`AI_API_KEY` 或数据库密码。

安全检查结果：

- 密码继续使用 BCrypt 保存。
- 注册接口强制角色为 `STUDENT`，请求中的角色字段不会被接受。
- JWT Secret、数据库密码和 AI Key 通过环境变量或本地未提交配置读取。
- 使用本地默认 JWT Secret 启动时输出 WARN，正式演示必须配置强随机 `JWT_SECRET`。
- Spring Security 继续保护所有未明确放行的 API。
- 角色注解和 Service 归属校验同时生效，组织者不能管理他人活动、查看他人名单或统计他人活动。
- MyBatis 用户输入使用参数绑定；keyword 不拼接原始 SQL；名单查询的 `${columns}` 仅接收服务端固定常量。

前端 `frontend/src/api/request.ts` 统一处理 400、401、403、409、5xx 和网络错误。401 会清理 Token 和用户信息并跳转登录页；400/409 保留后端业务 message；5xx 显示“系统繁忙，请稍后重试”；短时间重复错误会抑制重复弹窗；前端不输出 Token 到 console。

正式演示前检查：

1. 配置正确的 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
2. 已执行 `database/schema.sql` 和 `database/seed.sql`。
3. `JWT_SECRET` 已替换为高强度随机值，不能使用本地弱默认值。
4. `AI_MOCK_ENABLED=false`。
5. `AI_API_KEY` 已配置且不会提交到仓库。
6. 不提交 `.env`、真实密码、真实密钥或本地私有配置。

## 当前未实现内容

- 名单导出 Excel。
- 签到、二维码、报名审核和站内消息通知。
- 用户管理后台。
- 普通用户创建 `ORGANIZER` 或 `ADMIN` 的接口。

当前建议进入课程最终演示与提交材料整理；如现场演示要求真实 MySQL 脚本和真实 AI Key，请按 `docs/test-report.md` 与 `docs/final-acceptance-report.md` 中的风险项补做环境验证。

## 说明

- 本阶段没有修改 `database/schema.sql`、`database/seed.sql`、`database/reset.sql`。
- 当前项目目录不是 Git 仓库；如果需要版本管理，请先初始化或放入 Git 仓库后再提交。

