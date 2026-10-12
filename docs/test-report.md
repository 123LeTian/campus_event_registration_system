# 校园活动报名系统测试报告

测试日期：2026-10-12  
测试阶段：Phase 12 完整测试  
测试负责人：Codex

## 1. 测试结论

本阶段完成后端自动化测试、前端生产构建、当前开发实例 API 联调、前端关键页面浏览器验证和 30 条验收用例矩阵梳理。

结论：

- 后端 `mvn test` 通过：93 个测试，0 failures，0 errors，0 skipped。
- 前端 `pnpm build` 通过：TypeScript 和 Vite 构建均通过，无 chunk size warning。
- 当前 `http://127.0.0.1:8080` 后端实例可用，核心 API 主流程联调通过。
- 当前前端开发服务可通过 `http://localhost:5173` 访问并完成页面验证；`http://127.0.0.1:5173` 在本机测试时连接被拒绝，判断为当前 Node 服务仅绑定到 localhost/IPv6 的环境差异。
- 数据库 SQL 脚本验证未执行：MySQL 客户端可用，但当前 shell 未配置 `DB_PASSWORD`，`mysql -u root` 无密码登录失败。
- 真实 AI Key 调用未执行：当前 shell 未配置 `AI_API_KEY`。当前 8080 后端 AI 接口返回 Mock 风格内容，说明运行实例启用了可用的 AI 测试/Mock 行为；未验证真实大模型服务。
- 发现并修复 1 个前端移动端 Bug：统计页在 390px 手机视口横向溢出。已最小修改 CSS 并通过构建和页面回归。

代码层面可跳过 Phase 13 直接进入 Phase 14。进入最终验收前仍建议补做 MySQL SQL 脚本验证和真实 AI Key 验证。

## 2. 测试环境

| 项目 | 结果 |
| --- | --- |
| Java | OpenJDK 17.0.20.1 Temurin |
| Maven | Apache Maven 3.9.16，路径 `D:\DevEnv\Maven\apache-maven-3.9.16` |
| Node.js | v24.21.0 |
| pnpm | 12.6.0，路径 `D:\DevEnv\pnpm\pnpm.cmd` |
| MySQL 客户端 | MySQL 8.0.46 client 可用 |
| MySQL 登录 | `mysql --default-character-set=utf8mb4 -u root -e "SELECT VERSION()"` 失败，错误 1045，未配置密码 |
| Git | 当前目录不是 Git 仓库，未执行 reset、checkout、回滚 |
| 后端端口 8080 | 已有 Java 进程监听，`GET /api/health` 成功 |
| 前端端口 5173 | 已有 Node 进程监听；`localhost:5173` 成功，`127.0.0.1:5173` 拒绝连接 |
| DB_URL | 当前 shell 未配置 |
| DB_USERNAME | 当前 shell 未配置 |
| DB_PASSWORD | 当前 shell 未配置 |
| AI_API_KEY | 当前 shell 未配置 |
| AI_MODEL | 当前 shell 未配置 |
| AI_BASE_URL | 当前 shell 未配置 |
| AI_MOCK_ENABLED | 当前 shell 未配置；运行中的后端 AI 接口返回 Mock 风格内容 |

## 3. 自动化测试结果

执行命令：

```powershell
cd D:\Project\campus_event_registration_system\backend
D:\DevEnv\Maven\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：

| 指标 | 数量 |
| --- | ---: |
| Tests run | 93 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

覆盖模块：

- 注册、登录、JWT、当前用户。
- 角色权限、401/403 区分。
- 活动创建、编辑、删除、列表、详情。
- 搜索、状态筛选、分页、已删除活动权限。
- AI 活动介绍、宣传文案、Mock、Key 缺失、超时、异常格式。
- 报名、取消、重复报名、满员、并发抢名额、并发重复报名。
- 报名名单、我的报名。
- 统计概览、状态分布、报名率。
- 统一异常、安全和错误码。

说明：后端自动化测试使用 `test` profile 和 H2 内存库，不代表 MySQL SQL 脚本验证通过。

## 4. 前端构建结果

执行命令：

```powershell
cd D:\Project\campus_event_registration_system\frontend
D:\DevEnv\pnpm\pnpm.cmd build
```

结果：

- `vue-tsc --noEmit` 通过。
- `vite build` 通过。
- 构建产物正常生成到 `frontend/dist`。
- 未出现 chunk size warning。
- ECharts、Element Plus、Vue 分包仍生效。

## 5. 数据库脚本验证

状态：未执行。

原因：

- MySQL 客户端可用。
- 当前 shell 未配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
- 使用无密码 root 登录失败：`ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: NO)`。

未执行项：

- 创建或确认 `campus_event` 数据库。
- 执行 `database/schema.sql`。
- 执行 `database/seed.sql`。
- 重复执行 `seed.sql` 验证 Demo 用户不重复。
- 查询 Demo 用户数量与角色分布。
- 验证 `password` 字段为 BCrypt 哈希。
- 执行 `database/reset.sql` 验证只清空三张核心表数据。
- 再执行 `seed.sql` 验证 Demo 用户恢复。

后续人工验证命令：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

```sql
SET NAMES utf8mb4;
source D:/Project/campus_event_registration_system/database/schema.sql;
source D:/Project/campus_event_registration_system/database/seed.sql;
source D:/Project/campus_event_registration_system/database/seed.sql;
SELECT role, COUNT(*) FROM campus_event.`user` GROUP BY role;
SELECT username, password FROM campus_event.`user` WHERE username IN ('admin', 'organizer1', 'studentA', 'studentB', 'studentC', 'studentD');
source D:/Project/campus_event_registration_system/database/reset.sql;
source D:/Project/campus_event_registration_system/database/seed.sql;
```

## 6. 核心 API 联调

状态：通过。

环境说明：基于当前已运行的 `http://127.0.0.1:8080` 后端实例执行。按项目 dev 配置该实例应连接 MySQL；但因 SQL 客户端凭据缺失，本报告不把该结果等同于 SQL 脚本验证通过。

已验证流程：

| 项目 | 结果 |
| --- | --- |
| `GET /api/health` | 通过，返回 `Campus Event backend is running` |
| `POST /api/auth/login` organizer1 正确密码 | 通过 |
| organizer1 错误密码 | 通过，返回失败 |
| 注册新学生 | 通过，请求携带 `role=ADMIN` 时后端仍创建为 `STUDENT` |
| `GET /api/users/me` | 通过 |
| organizer1 创建活动 | 通过 |
| 活动列表搜索 | 通过 |
| 活动详情字段 | 通过 |
| AI 活动介绍 | 通过，当前运行实例返回 Mock 风格结构化内容；真实 AI 未验证 |
| student 调用 AI | 通过，返回 403 |
| student 报名 | 通过 |
| 重复报名 | 通过，返回 409 |
| 多学生报名到满员 | 通过，状态为 `FULL` |
| 满员后新学生报名 | 通过，返回 409 |
| 取消报名后名额恢复 | 通过 |
| 其他学生报名恢复名额 | 通过 |
| 组织者查看报名名单 | 通过 |
| 学生查看我的报名 | 通过 |
| 组织者查看统计 | 通过 |
| 学生创建活动 | 通过，返回 403 |
| 学生查看统计 | 通过，返回 403 |
| 报名截止后报名 | 通过，返回 409 |
| 删除活动后普通列表隐藏 | 通过 |

测试数据处理：

- API 联调创建了唯一前缀 `p12_...` 的学生账号，账号按系统设计保留。
- API 联调创建的测试活动已软删除。

## 7. 前端页面验证

状态：通过。

验证入口：

- `http://localhost:5173`
- `http://127.0.0.1:5173` 本机连接被拒绝，未作为页面验证入口。

浏览器验证工具：

- Playwright + 本机 Microsoft Edge，桌面视口约 1440×900，手机视口 390×844。

已验证页面和行为：

| 页面或行为 | 结果 |
| --- | --- |
| 登录页 | 通过 |
| 注册页 | 通过 |
| 工作台 | 通过 |
| 活动大厅搜索、状态筛选、分页 | 通过 |
| 活动详情 | 通过 |
| 学生活动详情报名/取消 | 通过 |
| 活动管理列表 | 通过 |
| 活动表单新增页 | 通过 |
| 活动表单编辑页 | 通过 |
| 活动表单 AI 辅助入口 | 通过 |
| 报名名单页 | 通过 |
| 我的报名页 | 通过 |
| 统计页图表 | 通过，canvas 非空 |
| 退出登录 | 通过，跳转登录页 |
| Token 失效 | 通过，清理会话并跳转登录页 |
| 学生账号无管理入口 | 通过 |
| 学生直接访问管理路由 | 通过，重定向活动大厅 |
| 组织者账号管理自己活动 | 通过 |
| 管理员账号进入管理和统计 | 通过 |
| 桌面横向溢出 | 通过 |
| 手机活动大厅横向溢出 | 通过，390px 视口下内容宽 375px |
| 手机统计页横向溢出 | 初测失败，修复后通过，390px 视口下内容宽 375px |

## 8. 验收用例矩阵

| 编号 | 用例 | 状态 | 依据 |
| --- | --- | --- | --- |
| AT-001 | 登录成功 | 通过 | API 联调和页面验证 |
| AT-002 | 创建活动 | 通过 | API 联调、后端自动化测试 |
| AT-003 | 非法人数 | 通过 | 后端自动化测试覆盖参数校验 |
| AT-004 | 空标题 | 通过 | 后端自动化测试覆盖参数校验 |
| AT-005 | 学生查看活动 | 通过 | 页面验证、后端自动化测试 |
| AT-006 | 正常报名 | 通过 | API 联调、页面验证 |
| AT-007 | 重复报名 | 通过 | API 联调、后端自动化测试 |
| AT-008 | 多人报名 | 通过 | API 联调、后端自动化测试 |
| AT-009 | 满员阻止 | 通过 | API 联调、后端自动化测试 |
| AT-010 | 取消报名 | 通过 | API 联调、页面验证 |
| AT-011 | 取消后他人报名 | 通过 | API 联调、后端自动化测试 |
| AT-012 | 查看名单 | 通过 | API 联调、页面验证 |
| AT-013 | 我的报名 | 通过 | API 联调、页面验证 |
| AT-014 | 搜索活动 | 通过 | API 联调、页面验证 |
| AT-015 | 状态筛选 | 通过 | 页面验证、后端自动化测试 |
| AT-016 | 查看统计 | 通过 | API 联调、页面验证 |
| AT-017 | 无权限删除 | 通过 | 后端自动化测试覆盖学生无管理权限；页面验证覆盖学生无管理入口 |
| AT-018 | 活动截止后报名 | 通过 | API 联调、后端自动化测试 |
| AT-019 | 删除活动 | 通过 | API 联调、后端自动化测试 |
| AT-020 | Token 失效 | 通过 | 页面验证、后端自动化测试 |
| AT-021 | AI 生成活动介绍 | 通过 | API 联调返回结构化内容；真实 AI 未验证 |
| AT-022 | AI 生成宣传文案 | 通过 | 后端自动化测试覆盖 Mock 宣传文案 |
| AT-023 | AI 内容修改后保存 | 通过 | 后端自动化测试覆盖 AI 结果人工确认后保存到活动介绍 |
| AT-024 | AI API 调用失败 | 通过 | 后端自动化测试覆盖服务不可用 |
| AT-025 | 学生无权调用 AI | 通过 | API 联调和后端自动化测试 |
| AT-026 | API Key 缺失 | 通过 | 后端自动化测试覆盖 Key 缺失；当前真实 Key 未配置 |
| AT-027 | AI 超时处理 | 通过 | 后端自动化测试覆盖超时 |
| AT-028 | 并发抢最后一个名额 | 通过 | 后端自动化测试覆盖并发 |
| AT-029 | 并发重复报名 | 通过 | 后端自动化测试覆盖并发 |
| AT-030 | 报名与取消并发 | 通过 | 后端自动化测试覆盖人数一致性 |

## 9. 已发现问题与修复记录

### 问题 1：手机视口下统计页横向溢出

问题描述：

- Playwright 在 390×844 手机视口验证统计页时发现 `documentElement.scrollWidth=853`，超过 `innerWidth=390`。
- 根因是统计明细表设置了 `min-width: 780px`，外层统计面板未限制横向滚动范围，导致整页被表格撑宽。

影响范围：

- 移动端统计页。
- 不影响后端接口和桌面主要流程。

修复文件：

- `frontend/src/styles.css`

修复内容：

- 为 `.statistics-content-panel` 和内部 `.statistics-panel` 增加 `min-width: 0`。
- 为 `.statistics-content-panel .statistics-table-panel` 增加 `overflow-x: auto`，把表格横向滚动限制在表格面板内。

回归命令：

```powershell
cd D:\Project\campus_event_registration_system\frontend
D:\DevEnv\pnpm\pnpm.cmd build
```

回归结果：

- 前端构建通过。
- Playwright 手机视口回归通过：统计页 `innerWidth=390`，`scrollWidth=375`，无整页横向溢出。

## 10. 未执行项与风险

未执行项：

- MySQL SQL 脚本验证未执行：缺少可用数据库密码。
- 真实 AI Key 调用未执行：当前 shell 未配置 `AI_API_KEY`。
- `http://127.0.0.1:5173` 页面入口未验证成功：当前 5173 服务通过 `localhost` 可访问，但 127.0.0.1 连接被拒绝。

剩余风险：

- 如果老师现场要求从空库执行 SQL 初始化，需要提前补做 `schema.sql`、`seed.sql`、`reset.sql` 的真实 MySQL 验证。
- 如果老师现场要求展示真实 AI，不应依赖 Mock，需要配置 `AI_API_KEY`、确认 `AI_MOCK_ENABLED=false` 并手工验证一次真实返回。
- 当前目录不是 Git 仓库，无法用 Git diff/commit 固化本阶段变更；如需提交材料，应先确认版本管理方式。

## 11. 最终建议

代码和页面层面的 Phase 12 验收重点已通过，发现的移动端统计页横向溢出已修复并回归。建议跳过 Phase 13 Bug 修复，进入 Phase 14 前补齐两个环境动作：

1. 使用真实 MySQL 凭据执行数据库脚本验证。
2. 配置真实 AI Key 并关闭 Mock，验证一次 AI 活动介绍和宣传文案。
