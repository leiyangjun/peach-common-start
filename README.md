# peach-common-start

**Spring Web/MVC 与 MyBatis 统一 Starter**：已合并原独立工程 **peach-mybatis**（通用 `BaseMapper`、`CommonSqlProvider` / `*SqlProvider`、分层模型基类、`PageHelper` 分页等）。Maven 坐标为 **`org.peach.common:peach-common-start`**；**Java 包名仍为 `org.peach.common.mybatis.*`**，业务代码无需改包，仅需将依赖从 `org.peach.common.mybatis:peach-mybatis` 改为本坐标。

统一通过 **`peach-dependencies` BOM** 管理版本；发布 Maven Central 的元数据已预置。

## 云与 Nacos（微服务）

- 启动类使用 **`@PeachCloud`**（`org.peach.common.mvc.annotation.start`，组合 `@SpringBootApplication` + `@EnableDiscoveryClient`）。
- 须在业务 POM **显式**引入 `spring-cloud-starter-alibaba-nacos-discovery`；使用配置中心时再引入 `spring-cloud-starter-alibaba-nacos-config` 并自行配置 `spring.cloud.nacos.*`、`spring.config.import` 等（按 Spring Boot 版本要求）。
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

## 注解摘要

- `@TableName`、`@ID`、`@LogicDelete`、`@TableExclude`、`@TableUnique`、`@SearchValue`、`@RangeQuery`  
- 分页：查询前 `PageSupport.startPage(queryDto)` 再执行列表 Mapper 方法。

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

