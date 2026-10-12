# 校园活动报名系统最终验收报告

验收日期：2026-10-12  
验收阶段：Phase 14 最终验收与交付整理  
项目路径：`D:\Project\campus_event_registration_system`

## 1. 项目概述

校园活动报名系统面向校园讲座、社团活动、竞赛和志愿活动，完成从活动发布、学生报名、人数控制、取消报名、名单查看到统计可视化的完整业务闭环。系统不是静态展示页面，核心数据来自数据库，核心规则由后端权限、事务、数据库约束和前端交互共同保障。

本阶段依据《校园活动报名系统开发设计文档》、老师验收标准、README 和 Phase 12 测试报告完成最终交付整理。当前目录不是 Git 仓库，本阶段未执行 `git reset`、`git checkout`、回滚或删除用户文件。

## 2. 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring Security 6、Spring Validation、MyBatis-Plus、JJWT、Maven |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus、ECharts |
| 数据库 | MySQL 8，测试环境使用 H2 内存库 |
| AI | OpenAI-compatible Chat Completions API，Java 17 HttpClient，支持 Mock 模式 |
| 构建工具 | Maven 3.9.16、pnpm 12.6.0 |

## 3. 已完成功能清单

| 模块 | 已完成功能 |
| --- | --- |
| 用户认证 | 注册、登录、JWT 签发、当前用户查询、退出登录、Token 失效处理 |
| 角色权限 | STUDENT、ORGANIZER、ADMIN 三类角色；前端入口控制和后端接口兜底校验 |
| 活动管理 | 活动创建、编辑、软删除、列表、详情、关键词搜索、状态筛选、分页 |
| 活动状态 | 后端统一计算 `REGISTRATION_OPEN`、`FULL`、`CLOSED`、`FINISHED`、`DELETED` |
| 报名业务 | 学生报名、取消报名、重复报名拦截、满员拦截、截止后拦截、取消后名额恢复 |
| 报名名单 | 组织者查看自己活动报名名单，管理员查看全部，支持分页和状态筛选 |
| 我的报名 | 学生查看自己的报名记录，支持有效、已取消和全部状态 |
| 统计可视化 | 统计概览、活动状态分布、活动报名率、活动统计明细 |
| AI 辅助创作 | 组织者/管理员生成活动介绍和宣传文案，AI 失败时允许手工填写 |
| 异常与安全 | 统一响应结构、错误码、参数校验、401/403/404/409/5xx 处理、安全日志 |
| 前端体验 | 响应式布局、移动端溢出修复、表单校验、错误提示、角色化导航 |

## 4. 角色与权限说明

| 角色 | 权限 |
| --- | --- |
| STUDENT | 查看活动大厅和活动详情；报名、取消报名；查看我的报名 |
| ORGANIZER | 创建、编辑、删除自己创建的活动；查看自己活动报名名单；查看自己活动统计；使用 AI 生成内容 |
| ADMIN | 管理全部活动；查看全部统计和名单；使用 AI 生成内容 |

权限边界：

- 普通注册用户固定为 `STUDENT`，请求体中的角色字段不会提升权限。
- `STUDENT` 不能创建、编辑、删除活动，不能查看报名名单、统计或调用 AI 接口。
- `ORGANIZER` 只能管理自己创建的活动，不能越权管理他人活动。
- `ADMIN` 拥有全局管理和统计权限。
- 前端隐藏无权限入口，后端使用 Spring Security 和业务归属校验兜底。

## 5. 核心 API 清单

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/health` | 匿名 | 健康检查 |
| POST | `/api/auth/register` | 匿名 | 用户注册，固定创建 STUDENT |
| POST | `/api/auth/login` | 匿名 | 用户登录，返回 JWT |
| GET | `/api/users/me` | 已登录 | 查询当前用户 |
| GET | `/api/events` | 已登录 | 活动列表，支持 keyword、status、mine、page、pageSize |
| GET | `/api/events/{id}` | 已登录 | 活动详情 |
| POST | `/api/events` | ORGANIZER、ADMIN | 创建活动 |
| PUT | `/api/events/{id}` | 创建者或 ADMIN | 编辑活动 |
| DELETE | `/api/events/{id}` | 创建者或 ADMIN | 软删除活动 |
| POST | `/api/events/{id}/registrations` | STUDENT | 报名活动 |
| DELETE | `/api/events/{id}/registrations/me` | STUDENT | 取消自己的报名 |
| GET | `/api/events/{id}/registrations` | ORGANIZER、ADMIN | 查看活动报名名单 |
| GET | `/api/my/registrations` | STUDENT | 查看我的报名 |
| GET | `/api/statistics/overview` | ORGANIZER、ADMIN | 统计概览 |
| GET | `/api/statistics/events` | ORGANIZER、ADMIN | 活动统计分页 |
| GET | `/api/statistics/status-distribution` | ORGANIZER、ADMIN | 活动状态分布 |
| POST | `/api/ai/event-introduction` | ORGANIZER、ADMIN | AI 生成活动介绍 |
| POST | `/api/ai/event-promotion` | ORGANIZER、ADMIN | AI 生成宣传文案 |

## 6. 数据库表说明

| 表 | 说明 | 关键约束 |
| --- | --- | --- |
| `user` | 用户表，保存账号、BCrypt 密码哈希、姓名、角色、启用状态 | `username` 唯一；`role` 限定 STUDENT/ORGANIZER/ADMIN |
| `event` | 活动表，保存标题、文案、介绍、地点、组织者、时间、人数、软删除状态 | 外键关联 `user.id`；人数上限大于 0；当前人数不超过上限；结束时间晚于开始时间；报名截止早于开始时间 |
| `registration` | 报名表，保存活动、学生、报名状态、报名和取消时间 | `(event_id, user_id)` 唯一；外键关联 `event` 和 `user`；状态限定 ACTIVE/CANCELLED |

数据库设计通过唯一索引、外键、CHECK 约束、事务和条件更新共同保障重复报名、满员、人数一致性和历史记录保留。

## 7. Demo 账号

| 用户名 | Demo 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin123456` | ADMIN |
| `organizer1` | `Org123456` | ORGANIZER |
| `studentA` | `Stu123456` | STUDENT |
| `studentB` | `Stu123456` | STUDENT |
| `studentC` | `Stu123456` | STUDENT |
| `studentD` | `Stu123456` | STUDENT |

说明：Demo 密码仅用于本地验收演示，数据库 `user.password` 字段保存 BCrypt 哈希，不保存明文。

## 8. 启动与验收步骤

### 8.1 数据库初始化

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

```sql
SET NAMES utf8mb4;
source D:/Project/campus_event_registration_system/database/schema.sql;
source D:/Project/campus_event_registration_system/database/seed.sql;
```

### 8.2 后端启动

```powershell
cd D:\Project\campus_event_registration_system\backend
$env:DB_URL="jdbc:mysql://localhost:3306/campus_event?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
D:\DevEnv\Maven\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

如需真实 AI 演示，额外配置：

```powershell
$env:AI_API_KEY="你的真实AI Key"
$env:AI_MODEL="gpt-4o-mini"
$env:AI_BASE_URL="https://api.openai.com/v1/chat/completions"
$env:AI_MOCK_ENABLED="false"
```

真实 Key 不应写入代码、README、测试报告或提交材料。

### 8.3 前端启动

```powershell
cd D:\Project\campus_event_registration_system\frontend
D:\DevEnv\pnpm\pnpm.cmd install
D:\DevEnv\pnpm\pnpm.cmd dev --host localhost
```

默认访问：

```text
http://localhost:5173
```

### 8.4 推荐现场演示路径

1. 使用 `organizer1 / Org123456` 登录。
2. 创建活动，展示活动表单、时间和人数校验。
3. 如配置真实 AI，生成活动介绍和宣传文案，修改后保存活动。
4. 使用 `studentA`、`studentB`、`studentC` 报名，展示人数变化和满员状态。
5. 使用 `studentD` 报名满员活动，展示满员失败。
6. `studentA` 取消报名，展示名额恢复。
7. `studentD` 再次报名成功。
8. 组织者查看报名名单。
9. 组织者或管理员查看统计图表。
10. 学生尝试访问管理或统计入口，展示无权限边界。

## 9. 自动化测试结果

执行命令：

```powershell
cd D:\Project\campus_event_registration_system\backend
D:\DevEnv\Maven\apache-maven-3.9.16\bin\mvn.cmd test
```

执行结果：

| 指标 | 数量 |
| --- | ---: |
| Tests run | 93 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| 构建结果 | BUILD SUCCESS |

覆盖范围包括认证、JWT、角色权限、活动管理、搜索筛选分页、AI Mock/异常、报名取消、重复报名、满员、并发、名单、我的报名、统计、统一异常和安全错误码。

## 10. 前端构建结果

执行命令：

```powershell
cd D:\Project\campus_event_registration_system\frontend
D:\DevEnv\pnpm\pnpm.cmd build
```

执行结果：

- `vue-tsc --noEmit` 通过。
- `vite build` 通过。
- 生产构建产物已生成到 `frontend/dist`。
- 未出现 chunk size warning。
- ECharts、Element Plus、Vue 相关分包正常。

## 11. MySQL 真实验证结果

真实 MySQL 脚本验证未执行，原因是当前环境未配置可用数据库账号或密码。

已确认事项：

- MySQL 客户端可用：MySQL 8.0.46 client。
- 当前 shell 未配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
- 执行 `mysql --default-character-set=utf8mb4 -u root -e "SELECT VERSION() AS version;"` 失败，返回 `ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: NO)`。

未执行事项：

- `database/schema.sql`。
- `database/seed.sql`。
- `database/reset.sql`。
- reset 后再次执行 `seed.sql`。
- 表存在性、Demo 用户数量、唯一约束、外键约束和 reset 行为验证。

建议在最终演示前使用真实 MySQL 密码补做上述验证。

## 12. AI 真实验证结果

真实 AI 验证未执行，原因是当前环境未配置 `AI_API_KEY`。

已确认事项：

- 后端自动化测试覆盖 AI Mock、AI Key 缺失、AI 超时、AI 服务异常、学生无权调用 AI。
- Phase 12 API 联调中，当前运行后端实例对 AI 介绍接口返回结构化 Mock 风格内容。
- 本阶段未向代码、README 或测试报告写入任何真实 Key。

未执行接口：

- `POST /api/ai/event-introduction` 的真实模型调用。
- `POST /api/ai/event-promotion` 的真实模型调用。

建议在最终演示前配置 `AI_API_KEY`，确认 `AI_MOCK_ENABLED=false`，并验证两条 AI 接口真实返回。

## 13. 已知风险与未实现功能

### 13.1 已知风险

| 优先级 | 风险 | 影响 | 建议 |
| --- | --- | --- | --- |
| P1 | MySQL 脚本未在当前环境真实验证 | 老师若要求从空库初始化，现场需证明 SQL 可执行 | 演示前用真实密码执行 schema、seed、reset、seed |
| P1 | 真实 AI Key 未验证 | 老师若要求真实 AI 返回，当前证据不足 | 配置 Key，关闭 Mock，验证介绍和宣传文案 |
| P2 | 当前目录不是 Git 仓库 | 无法用 Git diff/commit 固化交付状态 | 提交前按课程要求整理压缩包或初始化版本管理 |
| P2 | README 默认演示地址为 localhost | 部分机器可能需要 127.0.0.1 或显式 host | 如现场端口绑定异常，使用 Vite `--host localhost` 或按实际地址访问 |

### 13.2 未实现功能

以下功能不属于当前必做范围，未实现：

- Excel 导出。
- 签到、二维码。
- 报名审核。
- 用户管理后台。
- 站内消息通知。
- 智能问答或新的 AI 功能。
- Redis、消息队列、Docker、Kubernetes 等额外基础设施。

## 14. 最终验收结论

当前项目已满足校园活动报名系统的核心验收要求：

- 项目具备前后端和数据库设计，核心业务数据可持久化。
- 活动发布、编辑、删除、列表、详情、搜索、筛选、分页已完成。
- 学生报名、重复报名拦截、满员拦截、取消后名额恢复已完成。
- 报名名单、我的报名、统计可视化已完成。
- 角色权限、异常处理、安全边界和统一错误响应已完成。
- 后端自动化测试和前端构建均通过。
- Phase 12 测试报告和 Phase 14 最终验收报告已整理。

结论：在代码、构建、自动化测试和文档层面，当前项目可以交付老师验收。最终现场演示前，建议补做真实 MySQL 脚本验证和真实 AI Key 验证，以消除环境层面的剩余风险。
