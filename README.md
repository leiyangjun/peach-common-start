# peach-common-start

**Spring Web/MVC 与 MyBatis 统一 Starter**：已合并原独立工程 **peach-mybatis**（通用 `BaseMapper`、`CommonSqlProvider` / `*SqlProvider`、分层模型基类、`PageHelper` 分页等）。Maven 坐标为 **`org.peach.common:peach-common-start`**；**Java 包名仍为 `org.peach.common.mybatis.*`**，业务代码无需改包，仅需将依赖从 `org.peach.common.mybatis:peach-mybatis` 改为本坐标。

统一通过 **`peach-dependencies` BOM** 管理版本；发布 Maven Central 的元数据已预置。

## 工程说明

本模块为 **可复用 Spring Boot Starter（jar）**，聚合 **Spring WebMVC**、**MyBatis**、**Druid**、**PageHelper**、**springdoc-openapi**、**Jasypt**、校验 AOP、统一 `ApiResult` / 全局异常、可选 **读写分离数据源**、**Swagger/Nacos 元数据** 等能力。原独立工程 **peach-mybatis** 已合并至此，Java 包名为 **`org.peach.common.mybatis.*`**。

## 功能说明

- 通用 **BaseMapper** + 各类 `*SqlProvider` 动态 SQL。
- 实体与查询模型注解：`@TableName`、`@ID`、`@LogicDelete`、`@Exclude`、`@Unique`、`@SearchValue`、`@Range` 等。
- **分页**：`PageSupport` 与查询 DTO 衔接 PageHelper。
- **业务码**：`spring.application.module-code`（四位）与 `ApiResult` 拼装规则联动（见 `ModuleCodeCheckConfiguration`）。
- **登录用户**：经网关转发时从查询参数解析（`LoginUserUtil`，与 `peach-gateway` 注入的 `peach_*` 参数约定一致）。
- **代码生成**：内嵌 MyBatis Generator 封装，`GeneratorUtil` / `BaseMapperGeneratorUtil` 可从业务代码一键生成 Entity、Mapper 及 MVC 分层骨架。

## 云与 Nacos（微服务）

- 启动类使用 **`@PeachCloud`**（`org.peach.common.mvc.annotation.start`，组合 `@SpringBootApplication` + `@EnableDiscoveryClient`）。
- Nacos 注册发现、配置中心与 LoadBalancer 已由本 Starter **传递依赖**；下游仅需在 `application.yml` 填写 **`spring.cloud.nacos.server-addr/username/password`**（无 Starter 默认 IP/账号）。
- **`spring.profiles.active` 须由下游必填**（Starter 的 `application.yml` 不再提供默认值）；`ModuleCodeCheckConfiguration` 等会在无激活 profile 时启动失败。
- Nacos：`application.yml` 中 namespace 占位为 `${NACOS_NAMESPACE:${spring.profiles.active}}`；若未设 `NACOS_NAMESPACE`，须先有有效 profile，否则 Starter 不会在早期注入 namespace（避免静默连 public）。其余约定（group、DataId=`${spring.application.name}.yaml` 等）由 Starter 默认提供；关闭：`spring.cloud.nacos.discovery.enable=false` / `spring.cloud.nacos.config.enable=false`（默认均为开启）。
- **未引入本 Starter** 的工程（如独立 Gateway）请自行使用 `@SpringBootApplication` / `@EnableDiscoveryClient` 等。

## 坐标

| 项 | 值 |
| --- | --- |
| `groupId` | `org.peach.common` |
| `artifactId` | `peach-common-start` |

## 依赖引入

业务工程 `import` `peach-dependencies` 后：

```xml
<dependency>
  <groupId>org.peach.common</groupId>
  <artifactId>peach-common-start</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 核心内容（MyBatis 能力，包名 org.peach.common.mybatis）

| 包 | 说明 |
| --- | --- |
| `org.peach.common.mybatis.mapper.BaseMapper` | 通用 Mapper（`@*Provider` → `InsertSqlProvider` 等） |
| `org.peach.common.mybatis.mapper.CommonSqlProvider` | 元数据/SQL 片段工具；`*SqlProvider` 为 MyBatis 动态 SQL 入口 |
| `org.peach.common.mybatis.model.*` | Entity / BO / DTO / VO / Query 基类 |
| `org.peach.common.mybatis.page.PageSupport` | `PageHelper` 与 `BaseQueryDto` 衔接 |

## 注解摘要（MyBatis 域）

| 注解 | 包路径 |
| --- | --- |
| `@TableName` | `org.peach.common.mybatis.annotation` |
| `@ID` | 同上 |
| `@LogicDelete` | 同上 |
| `@Exclude`（表字段排除） | 同上 |
| `@Unique` | 同上 |
| `@SearchValue`（模糊查询） | 同上 |
| `@Range`（范围查询，与精确条件组合） | 同上 |

**树形结构辅助**（`org.peach.common.utils.annotation`）：`@TreeId`、`@TreeParentId`、`@TreeSortField`，配合 `TreeUtil`。

**其它**：`org.peach.common.mvc.annotation.json.Sensitive`（脱敏序列化）；`org.peach.common.mvc.validation.AutoValidated`（与 `ValidAspect` 配合）。

- 分页：查询前 `PageSupport.startPage(queryDto)`（`org.peach.common.mybatis.page`）再执行列表 Mapper 方法。

## Spring Boot 自动配置（`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）

| 配置类 | 作用摘要 |
| --- | --- |
| `PeachLoggingAutoConfiguration` | 日志相关自动配置 |
| `NacosMetadataAutoConfiguration` | 存在 Nacos Discovery 时写入实例元数据（OpenAPI 等） |
| `ModuleCodeCheckConfiguration` | 校验 `spring.application.module-code` 与激活 profile（须非空） |
| `ReadWriteDataSourceAutoConfiguration` | `spring.datasource.rw.enabled` 读写分离路由 |
| `MybatisLogAutoConfiguration` | MyBatis SQL 日志 |
| `ApisAutoConfiguration` | 通用 `ApisController` 等 |
| `MvcExceptionAutoConfiguration` | 全局异常 → `ApiResult` / `ErrorResult` |
| `SpringBeanUtilAutoConfiguration` | `SpringBeanUtil` 单例 |
| `ValidAutoConfiguration` | 参数校验切面 |
| `SensitiveJacksonAutoConfiguration` | `@Sensitive` Jackson 模块 |
| `SwaggerAutoConfiguration` | springdoc / Swagger UI 约定 |

## 代码生成（`org.peach.common.mybatis.generator`）

依赖已包含 **`mybatis-generator-core`**，业务可在**测试类、临时 main、或一次性工具类**中调用（注意执行目录一般为模块根，以便写入 `src/main/java`）。

**推荐**：在业务模块根目录下调用，使「根包名」为调用类所在包（内部通过栈解析 `BaseMapperGeneratorUtil.resolveCallerBasePackage()`）。

```java
// 表名可变参数；jdbcDriverClass 可为 null，由 URL 推断驱动类
GeneratorResult r = GeneratorUtil.generateAll(
    "jdbc:postgresql://127.0.0.1:5432/peach_common",
    "postgres",
    "postgres",
    null,
    "cmn_user", "cmn_role");

// CI / 多模块：显式指定源码根与 basePackage
GeneratorUtil.generateAll(jdbcUrl, user, pass, null,
    Path.of("/abs/path/to/module/src/main/java"),
    "org.peach.common",
    List.of("cmn_user"));
```

底层还可使用 `BaseMapperGeneratorUtil`、`BaseMapperGeneratorRequest` 细粒度控制；单元测试见 `src/test/java/org/peach/common/mybatis/generator/BaseMapperGeneratorUtilTest.java`（不连真实库）。

## 常用工具类（入口指引）

| 类 | 包 | 说明 |
| --- | --- | --- |
| `BeanUtil` | `org.peach.common.utils` | 对象拷贝等 |
| `JSONUtil` | 同上 | JSON 与对象转换 |
| `Base64Util` | 同上 | Base64 |
| `IdUtil` | 同上 | 雪花 ID；JVM 参数 `peach.mybatis.snowflake.worker-id` / `datacenter-id`（默认 1） |
| `TreeUtil` | 同上 | 列表转树 |
| `LoginUserUtil` | 同上 | 从当前请求读取网关注入的用户查询参数 |
| `SpringBeanUtil` | `org.peach.common.mvc` | 非注入场景获取 Spring Bean |

## 配置与环境变量（摘要）

- **`spring.profiles.active`**：须由业务工程显式配置（或通过 `SPRING_PROFILES_ACTIVE` 等标准方式传入）；Starter 不负责默认激活 profile。
- **`spring.application.module-code`**：四位模块码，必填（与网关、各服务 `application.yml` 中示例一致：`AUTH`、`COMM`、`GWAY` 等）。
- **读写分离**：见 sibling **`peach-common-service`** 的 `application.yml` 注释（`spring.datasource.rw.*`）；开启后写库 / `@Transactional(readOnly=true)` 读库路由由 `ReadWriteDataSourceAutoConfiguration` 接管。
- **Jasypt**：已引入 `jasypt-spring-boot-starter`，敏感配置可按 Jasypt 官方方式加密（本 Starter 不负责具体密钥分发方式）。

## 开发约定

- **包名**：MyBatis 相关统一为 `org.peach.common.mybatis`（勿与历史 `org.peach.mybatis` 混淆）。
- **可选依赖**：Nacos 在 `pom.xml` 中为 `optional`，使用 `@PeachCloud` 的业务工程须**显式**添加 `spring-cloud-starter-alibaba-nacos-discovery`。
- **异常**：业务优先抛 `BizException`（`org.peach.common.mvc.exception`），配合 `CrudBizCode` / `MessageCode` 体系。
- **OpenAPI**：使用 `swagger-annotations-jakarta` 等 jakarta 命名空间，与 Spring Boot 4 对齐。

## 后续工作

1. 若需 Spring MVC 统一响应/异常等，可在本工程同包或 `org.peach.common.*` 下扩展；依赖由 BOM 约束版本。
2. 发布 Central 前可补 `maven-source-plugin` / `maven-javadoc-plugin`。
3. 发布：`mvn clean deploy -Prelease-sign`（需 `central` 凭据与 GPG）。

## 本地构建

```bash
mvn -f peach-dependencies/pom.xml install -DskipTests
mvn -f peach-common-start/pom.xml clean verify
```

## 作者

leiyangjun

