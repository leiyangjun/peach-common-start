# Peach 开发平台现状评估与建设建议报告

| 项 | 内容 |
| --- | --- |
| **文档版本** | v1.0 |
| **编制日期** | 2026-05-15 |
| **启动器根目录** | `peach-common-start` |
| **评估范围** | 工作区 `d:\eclipse-workspace` 内 Peach 体系各工程当前代码雏形 |

---

## 1. 文档说明

### 1.1 目的

本报告基于 **2026-05-15** 时点对 Peach 开发平台各仓库源码、README、SQL 脚本与测试资产的梳理，对平台**已具备能力**、**明显短板**及**分阶段建设路径**作出正式评估，供技术决策、立项排期与对外沟通使用。

### 1.2 适用范围

| 读者 | 用途 |
| --- | --- |
| 架构师 / 技术负责人 | 把握整体成熟度、识别风险与投入优先级 |
| 后端 / 前端开发 | 理解工程边界、约定与待补齐能力 |
| 运维 / 交付 | 评估上线前置条件与环境依赖 |
| 产品 / 项目管理 | 对齐「平台」与「业务系统」的能力边界 |

### 1.3 评估依据

本报告**不依赖外部商业方案对照**，结论均来自当前工作区内可运行或可构建的工程，主要包括：

| 工程 | 角色 |
| --- | --- |
| `peach-dependencies` | Maven BOM，统一 JDK 21 / Spring Boot 4 / Spring Cloud 版本线 |
| `peach-common-start` | 统一 Starter（MVC、MyBatis、ApiResult、读写分离、代码生成等） |
| `peach-gateway` | API 网关（JWT 校验、Nacos 动态路由、文档门户） |
| `peach-auth-service` | 认证服务（登录、JWT 签发） |
| `peach-common-service` | 基础业务服务（用户、角色、菜单、字典、权限配置） |
| `peach-job-service` | 定时任务服务（Quartz JDBC + HTTP 调度） |
| `peach-admin-web` | 管理端前端（Vue 3 + Vite + Element Plus） |
| `peach-actuator` | 监控扩展占位工程（尚无实现） |

> **说明**：各工程当前为**多独立 Git 仓库**并列于同一工作区；部分能力仍在「骨架 + 核心路径打通」阶段，下文「已具备」指主链路可联调，「薄弱/缺失」指生产级或平台化仍不足。

---

## 2. 当前已具备能力

### 2.1 管理端（`peach-admin-web`）

| 能力项 | 现状 |
| --- | --- |
| 技术栈 | Vue 3、Vite 8、TypeScript、Element Plus、Pinia、Vue Router |
| 架构分层 | 已建立 `models` / `controllers` / `views` MVC 分层约定（见 `MVC-ARCHITECTURE.md`） |
| 业务页面 | 登录、首页、用户、角色、菜单、字典、定时任务（列表 + 编辑 + Cron 配置） |
| 网关联调 | 双 HTTP 客户端：`/api` → 认证服务、`/api-common` → 基础服务；管理上下文前缀 `/admin` 与后端 `peach.api.context` 对齐 |
| 权限配置 UI | 菜单按钮字典、按钮 API 绑定、角色菜单按钮绑定、API 资源穿梭选择（对接注册中心 admin API 目录） |
| 认证体验 | Bearer Token 持久化、RSA 口令加密、滑块验证组件 |

**评价**：管理端已覆盖「系统管理 + 任务调度」主场景，交互链路完整；侧栏菜单当前以静态配置与库表种子并存，动态菜单与按钮级前端鉴权尚未闭环。

### 2.2 权限体系（`peach-common-service` + Starter）

| 层级 | 现状 |
| --- | --- |
| 身份认证 | `peach-auth-service` 负责登录与 JWT 签发；网关 `TokenGlobalFilter` 统一验签 |
| 身份传递 | 网关将 JWT `sub` 展开为 `peach_user_id`、`peach_username` 等查询参数；业务侧 `LoginUserUtil` 读取 |
| RBAC 数据模型 | 用户、角色、菜单（含目录/菜单/外链类型）、菜单按钮、按钮 API、角色按钮绑定 |
| 权限配置 API | `PermissionController` 聚合按钮字典、菜单/角色绑定、注册服务列表、admin API 元数据拉取 |
| API 分层 | `@AdminApi` / `@AppApi` / `@OpenApi` + `peach.api.context` 路径前缀（admin 默认 `/admin`） |
| 字段级审计辅助 | MyBatis `InsertSqlProvider` / `UpdateSqlProvider` 支持 creator/editor 等审计列自动填充 |

**评价**：权限**配置面**较完整，具备「菜单 → 按钮 → API」三元绑定能力；**运行时 API 级鉴权**（按角色按钮拦截请求）、**数据权限**（行级/组织级过滤）尚未在网关或业务拦截层落地。

### 2.3 API 网关（`peach-gateway`）

| 能力项 | 现状 |
| --- | --- |
| 路由 | 基于 Nacos 服务发现的动态路由（`/{serviceId}/**`），支持路由表轮询刷新 |
| 鉴权 | 全局 JWT 校验；匿名白名单（登录、Swagger、路由调试等）可维护 |
| 跨域 | CORS 支持 |
| 文档 | 网关本机 springdoc + `/peach-doc-portal` 微服务文档聚合入口 |
| 错误模型 | 11 位业务码 `ErrorResult`（模块码 + HTTP 段 + 语义段），与业务服务 `ApiResult` 区分清晰 |
| 测试 | 过滤器匿名路径、动态路由、文档门户等单元测试已覆盖 |

**评价**：网关作为统一入口的职责清晰，适合作为「北向流量」收口点；**限流、熔断、灰度、WAF 级防护**尚未集成。

### 2.4 定时任务（`peach-job-service`）

| 能力项 | 现状 |
| --- | --- |
| 调度引擎 | Quartz JDBC 持久化（PostgreSQL），启动时全量同步 `job_task` |
| 执行模型 | 统一 `HttpJob`：**仅支持 GET**；INTERNAL 经 Nacos + LoadBalancer 访问 `http://{serviceId}`；EXTERNAL 直连基址 |
| 管理 API | 任务 CRUD、暂停/恢复/触发、调度刷新、执行日志查询 |
| API 发现 | 按 `serviceId` 拉取各服务 admin API 目录（负载均衡直连，不经网关） |
| 前端 | 任务列表、编辑页、Cron 可视化、API 资源穿梭绑定 |
| 数据库 | 提供 `00_create_database.sql`、`01_init_peach_job_schema.sql` 与旧库增量补丁 |

**评价**：「配置化 HTTP 定时调用」主链路已通；鉴权依赖任务级 `headers` 或全局 `peach.job.http-authorization`，**无服务账号体系**；集群调度、失败告警、非 GET 方法均未支持。

### 2.5 工程结构与技术底座

```
peach-dependencies          ← BOM（版本中枢）
        │
        ├── peach-common-start    ← 可发布 Starter（Web / MyBatis / 统一响应 / 生成器）
        │
        ├── peach-gateway           ← 独立 WebFlux 网关（不依赖 Starter）
        ├── peach-auth-service      ← 认证微服务
        ├── peach-common-service    ← 基础业务微服务
        ├── peach-job-service       ← 任务微服务
        ├── peach-actuator          ← 监控 Starter 占位
        │
        └── peach-admin-web         ← 前端 SPA（独立 npm 工程）
```

| 维度 | 现状 |
| --- | --- |
| 语言与框架 | JDK 21、Spring Boot 4.0.x、Spring Cloud 2025.1.x、Spring Cloud Alibaba |
| 注册发现 | Nacos Discovery（各服务显式引入） |
| 数据访问 | 自研 `BaseMapper` + SqlProvider 动态 SQL、PageHelper 分页、逻辑删除、树形工具 |
| 统一响应 | `ApiResult` / `BizException` / 四位 `module-code` 业务码体系 |
| 配置加密 | Jasypt Starter 已引入（密钥分发策略待规范） |
| 代码生成 | Starter 内嵌 MyBatis Generator 封装，可生成 Entity / Mapper / MVC 骨架 |
| 读写分离 | `ReadWriteDataSourceAutoConfiguration` 可选开启 |
| 发布 | `peach-dependencies`、`peach-common-start` 等已预置 Maven Central 发布元数据 |

**评价**：技术选型现代、Starter 抽象程度高，**适合作为多条业务线的公共底座**；工程间版本通过 BOM 对齐，但**仓库物理分散**，全栈联调依赖本地 `mvn install` 顺序与文档约定。

---

## 3. 明显缺失或薄弱项

### 3.1 平台工程化

| 薄弱项 | 现状 | 风险 |
| --- | --- | --- |
| **CI/CD** | 各仓库未见 GitHub Actions / GitLab CI / Jenkinsfile；构建与测试依赖手工执行 | 质量不可追溯，回归成本高 |
| **数据库迁移** | 以 `init_*.sql` + 手工 `patch_*.sql` 为主；未引入 Flyway / Liquibase | 多环境 schema 漂移、升级不可重复 |
| **环境文档** | README 分散在各工程；环境变量表不完整统一 | 新人上手与交付周期长 |
| **API 契约** | 依赖 springdoc 运行时文档；无 OpenAPI 导出校验、无契约测试、无版本化 API 治理 | 前后端与多服务联调易出现隐性破坏 |
| **依赖与镜像** | 无统一 Docker Compose / Helm Chart 描述全栈 | 环境复制困难 |

### 3.2 安全与服务间调用

| 薄弱项 | 现状 | 风险 |
| --- | --- | --- |
| **Job 鉴权** | 定时任务 HTTP 调用靠静态 Header 或全局 Authorization；无服务账号、无 Token 自动刷新 | 生产密钥泄露面大；Token 过期导致任务静默失败 |
| **操作审计** | 仅有表字段 creator/editor 自动填充；**无操作日志**（谁、何时、对何资源、旧值/新值） | 合规与追责能力不足 |
| **数据权限** | 无组织/部门/租户维度的行级过滤 | 多团队共用平台时数据隔离不足 |
| **JWT 密钥** | 网关与认证服务仍共享代码内默认 HS256 密钥（README 已警示） | 生产环境重大安全隐患 |
| **运行时权限** | RBAC 配置完整，但业务 API 层未见统一「按钮/API 权限」拦截器 | 配置与 enforcement 脱节，越权风险 |
| **服务间 mTLS** | 未涉及 | 内网横向移动防护不足 |

### 3.3 可观测与运维

| 薄弱项 | 现状 | 风险 |
| --- | --- | --- |
| **监控指标** | `peach-actuator` 为空壳；各服务未统一暴露 Micrometer/Prometheus 约定 | 无法容量规划与 SLA 管理 |
| **链路追踪** | `TraceIdFilter` 存在，但未见与 Zipkin/Jaeger/OTel 集成说明 | 跨服务问题定位困难 |
| **Job 告警** | 执行日志落库，无失败率告警、无通知渠道（邮件/企微/钉钉） | 定时任务故障发现滞后 |
| **网关限流** | 无限流、熔断、舱壁；README 亦指出无 Resilience4j/Sentinel | 流量尖峰或慢服务可拖垮全链路 |
| **日志规范** | 各服务独立 `logs/`；无集中采集与检索方案 | 故障排查效率低 |
| **健康检查** | 未形成统一的 readiness/liveness 与依赖检查（DB、Nacos、Redis） | K8s 编排就绪度不足 |

### 3.4 产品化模块

| 模块 | 现状 |
| --- | --- |
| **多租户** | 未实现 |
| **文件存储** | 未实现（无 OSS/MinIO 抽象） |
| **消息通知** | 未实现（站内信、短信、邮件统一服务） |
| **工作流** | 未实现 |
| **代码生成（产品化）** | Starter 提供开发期生成器，**无**在线代码生成管理界面 |
| **国际化（i18n）** | 前端与后端错误消息均未体系化 i18n |
| **字典/配置中心** | 字典 CRUD 已有；无动态配置下发与灰度 |
| **开放平台** | `@OpenApi` 注解与路径分层已预留，无开发者门户、应用密钥、配额 |

### 3.5 前端体验规范化

| 薄弱项 | 说明 |
| --- | --- |
| 设计规范 | 无统一 Design Token、间距/色彩/表单交互规范文档 |
| 菜单来源 | 静态侧栏与库表菜单种子并存，动态权限菜单未完全驱动路由 |
| 按钮级权限 | 页面内操作按钮未与角色按钮绑定联动禁用/隐藏 |
| 全局能力 | 无统一表格/表单/详情脚手架；各页风格接近但未组件化沉淀 |
| 大数与精度 | 已引入 `json-bigint`，但无全局策略说明 |
| 无障碍与主题 | 未涉及 |

### 3.6 测试与质量

| 维度 | 现状 |
| --- | --- |
| 后端单测 | `peach-common-start`、`peach-gateway`、`peach-auth-service` 有部分单元测试；`peach-common-service`、`peach-job-service` 覆盖薄弱 |
| 集成测试 | 未见 Testcontainers / 全链路集成测试 |
| 前端测试 | `package.json` 无 `test` 脚本；无 Vitest/Cypress |
| 静态扫描 | 未见 Sonar、Checkstyle、ESLint CI 门禁 |
| 性能测试 | 未涉及 |

---

## 4. 建议推进优先级（三阶段）

### 第一阶段：可交付基线（约 4～8 周）

**目标**：具备「可重复部署、可安全上线、可追责」的最小生产形态。

| 序号 | 事项 | 产出 |
| --- | --- | --- |
| P0 | JWT 密钥外置化（环境变量 / Nacos 配置），网关与认证同步 | 安全配置规范 |
| P0 | 引入 Flyway（或 Liquibase），统一 `peach-common`、`peach-job` 等库迁移 | 可版本化 DB 升级 |
| P0 | 根目录或独立 `peach-devops`：Docker Compose（PG + Nacos + 核心服务 + 网关 + 前端静态） | 一键本地/测试环境 |
| P1 | CI：各仓库 `mvn verify` / `npm run build` + 单测门禁 | 主干质量红线 |
| P1 | 操作审计表 + AOP 记录关键 admin 写操作 | 审计查询 API |
| P1 | 统一 README「全栈启动顺序」与环境变量总表 | 交付文档 |
| P2 | API 运行时权限拦截（基于角色按钮已绑 API 元数据） | 配置与 enforcement 闭环 |

### 第二阶段：平台化增强（约 8～16 周）

**目标**：降低业务线接入成本，提升运维可观测性。

| 序号 | 事项 | 产出 |
| --- | --- | --- |
| P0 | 实现 `peach-actuator`：统一 Actuator 端点、Prometheus 指标、健康检查 | 监控大盘接入 |
| P0 | Job 服务账号 + 短期 Token 或内部签名；失败告警 Webhook | 任务可靠性与通知 |
| P1 | 网关限流（Redis/本地令牌桶）+ 可选熔断 | 流量防护 |
| P1 | OpenAPI 导出 + 契约 diff（CI） | API 治理 |
| P1 | 前端：动态菜单驱动路由 + 按钮级 `v-permission` | 权限体验一致 |
| P2 | 文件服务（MinIO 适配层） | 通用上传下载 |
| P2 | 消息服务骨架（站内信 + 异步投递） | 通知扩展点 |
| P2 | `HttpJob` 扩展 POST/PUT + Body 模板 | 调度场景覆盖 |

### 第三阶段：产品化与生态（约 16 周以后）

**目标**：形成可对外宣传的「低代码 / 多租户企业开发平台」。

| 序号 | 事项 | 说明 |
| --- | --- | --- |
| P1 | 多租户（Schema 或行级 `tenant_id`） | 与数据权限联动 |
| P1 | 在线代码生成与模块脚手架 | 将 Starter 生成器产品化 |
| P2 | 工作流引擎集成（Flowable/Camunda 等） | 审批类场景 |
| P2 | 国际化与错误码字典化 | 前后端统一 |
| P3 | 开发者门户（OpenAPI 应用、配额、审计） | 开放平台 |
| P3 | Monorepo 或 Meta-Repo 评估落地 | 见第 5 节 |

---

## 5. 架构取舍

### 5.1 多仓库 vs Monorepo

| 维度 | 当前多仓库 | Monorepo（建议中期评估） |
| --- | --- | --- |
| **现状** | `peach-gateway`、`peach-common-start`、`peach-admin-web` 等各自独立 Git，双推 GitHub/Gitee | 工作区仅物理并列，无统一版本标签 |
| **优点** | 模块边界清晰；Starter/BOM 可独立发版到 Maven Central | 一次 PR 可跨前后端联调；统一 CI/CD 与变更记录 |
| **缺点** | 联调需本地 `install` 顺序；跨库 Issue/版本对齐成本高 | 仓库体积与权限粒度变粗；需工具（Maven Reactor / Nx / Turborepo） |
| **建议** | **短期维持多仓库**，靠 BOM 版本号 + 发布说明对齐 | **当服务数 > 6 且每周多次跨模块变更时**，引入 Meta-Repo（如 `peach-platform`）聚合子模块 Git Submodule 或 Maven Reactor，**不必一次性合并历史** |

### 5.2 定时任务仅支持 GET 的扩展性

| 议题 | 分析 |
| --- | --- |
| **当前设计** | `HttpJob` 固定 `HttpMethod.GET`，适合「触发同步、清理缓存、拉取报表」等幂等读操作 |
| **局限性** | 无法承载「POST 创建单据」「PUT 状态推进」；带 Body 的回调、GraphQL、消息投递均不支持 |
| **安全考量** | GET 语义不应产生副作用；若用 GET 模拟写操作，违背 HTTP 规范且不利于缓存与重试语义 |
| **扩展建议** | 在 `job_task` 增加 `http_method`、`body_template`、`content_type`；Quartz 执行层按配置构造 `RestTemplate.exchange`；**写操作强制幂等键或服务间签名** |
| **取舍结论** | **现阶段 GET-only 有利于降低复杂度与安全面**；应在第二阶段按需扩展，并与 Job 鉴权、操作审计同步建设 |

---

## 6. 平台定位一句话总结

> **Peach 是一套基于 Spring Cloud 微服务架构、以统一 Starter 与 RBAC 权限模型为内核的企业级开发底座，已打通「认证 — 网关 — 基础管理 — 定时调度 — 管理前端」主链路，尚处于可联调雏形向可生产交付演进的关键阶段。**

---

## 7. 附录：最小可上线清单（建议）

以下条目可作为首个生产环境上线的**验收核对表**（勾选即视为达标）。

### 7.1 基础设施

- [ ] PostgreSQL 高可用或托管实例已就绪（`peach_common`、`peach_job` 等库已迁移至最新 Flyway 版本）
- [ ] Nacos 集群已部署，命名空间与分组与生产一致
- [ ] 反向代理（Nginx/Ingress）将 `/api`、`/api-common` 转发至 `peach-gateway`
- [ ] TLS 证书已配置（对外 HTTPS）

### 7.2 安全

- [ ] JWT HS256 密钥已更换为强随机值，且仅存在于配置中心/密钥管理服务
- [ ] 数据库、Nacos、Redis 等凭据已加密存储（Jasypt 或云厂商密钥管理）
- [ ] 网关匿名路径清单已评审，无多余开放接口
- [ ] 管理端登录启用滑块/RSA 等现有防护，口令策略已约定
- [ ] 定时任务不使用长期有效的用户 JWT 作为 `Authorization`（改为服务账号或内部签名）

### 7.3 应用与数据

- [ ] `peach-auth-service`、`peach-common-service`、`peach-gateway`、`peach-job-service` 均已注册 Nacos 且健康
- [ ] `peach-admin-web` 静态资源已构建（`npm run build`）并托管
- [ ] 初始化数据（管理员账号、菜单、角色）已执行且默认口令已修改
- [ ] 数据库备份策略与恢复演练已完成

### 7.4 可观测与运维

- [ ] 各服务日志集中采集（至少可按 `traceId`、服务名检索）
- [ ] JVM/HTTP 基础指标已接入监控（CPU、内存、QPS、延迟、错误率）
- [ ] 定时任务连续失败告警已配置
- [ ] 发布/回滚流程文档化（含 DB 迁移回滚策略）

### 7.5 质量门禁

- [ ] 主干 CI 通过：`mvn verify`（后端）、`npm run build`（前端）
- [ ] 核心路径冒烟用例通过：登录 → 用户列表 → 角色授权 → 创建定时任务 → 手动触发
- [ ] 已知高危项（无 API 运行时鉴权、无操作审计等）已纳入上线后第一批迭代，或已接受风险备案

---

## 文档修订记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.0 | 2026-05-15 | 首版：基于工作区全量工程扫描编制 |

---

*本报告存放于启动器根目录 `peach-common-start/peach-module-start.md`，随平台演进应定期复审更新。*
