package org.peach.common.openapi.autoconfigure;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * SpringDoc 集成入口：导入 {@link PeachOpenApiConfiguration}（普通 {@link org.springframework.context.annotation.Configuration}，
 * 内声明 {@link io.swagger.v3.oas.models.OpenAPI} 与 {@link org.springdoc.core.customizers.OpenApiCustomizer}）。
 * <p>
 * <b>为何「在 common 里手写 {@code @Bean OpenAPI}」能显示联系人，走 starter 有时感觉不行？</b>
 * </p>
 * <ul>
 * <li><b>手写配置类</b>：放在 {@code org.peach.common.*} 等包下时，会被 {@code @SpringBootApplication} 的<strong>组件扫描</strong>加载，
 * 只要类在 classpath 且未被过滤，配置类里的 Bean 一定会注册——这是最直观的路径。</li>
 * <li><b>Starter 自动配置</b>：入口类 {@link SwaggerAutoConfiguration} 只能通过
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 被 Boot 拉起来；若工程里写了
 * {@code spring.autoconfigure.exclude} 排除了本类，或条件注解（如仅 Servlet Web）不满足，则<strong>整段不会加载</strong>，
 * 与「扫描到的配置类」相比更容易被误伤。</li>
 * <li>历史上 starter 曾使用 {@code @ConditionalOnMissingBean(OpenAPI.class)}：一旦上下文里已有别的 {@link io.swagger.v3.oas.models.OpenAPI}
 * Bean，整段 {@code peachOpenApi} 会被跳过，文档基础信息（含 {@code contact}）就不再来自 starter。</li>
 * <li>当前做法：{@link PeachOpenApiConfiguration} 中 {@code peachOpenApi} 使用 {@link org.springframework.context.annotation.Primary}，
 * 且不再使用 {@code ConditionalOnMissingBean(OpenAPI.class)}，与手写单 Bean 行为对齐；若业务仍需自定义全局 {@link io.swagger.v3.oas.models.OpenAPI}，
 * 请在其 Bean 上再加 {@link org.springframework.context.annotation.Primary} 覆盖。</li>
 * <li>若页面上仍看不到联系人，请<strong>直连微服务</strong>打开 {@code /v3/api-docs} 看 JSON 是否含 {@code info.contact}：JSON 有而 UI 无，多为 Swagger UI 缓存或版本展示差异。</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenApiCustomizer.class)
@Import(PeachOpenApiConfiguration.class)
public class SwaggerAutoConfiguration {
}
