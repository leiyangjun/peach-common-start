package org.peach.common.mvc.openapi.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 为 SpringDoc 提供 Starter 级默认（业务工程无需写 springdoc 段落）。
 * 实现 {@link org.springframework.boot.EnvironmentPostProcessor}（Boot 4 推荐接口）。
 * <p>
 * 默认值通过本类在环境准备阶段注入，而非 Starter 的 {@code application.yml}：依赖 jar 时，
 * classpath 上多个 {@code application.yml} 的合并顺序与是否被业务配置覆盖并不可靠，用户实测
 * starter 内 springdoc 段落在部分场景下不生效；{@link EnvironmentPostProcessor} + {@code addLast}
 * 的 {@link MapPropertySource} 可在业务未显式配置时稳定提供平台默认。
 * </p>
 * <ul>
 * <li>关闭 OpenAPI 文档缓存，避免先直连再经网关时沿用错误的 {@code servers}；</li>
 * <li>开启 Swagger UI {@code persistAuthorization}，刷新页面后保留 Authorize 中填写的 Token；</li>
 * <li>不覆盖 {@code springdoc.swagger-ui.path}：SpringDoc 官方入口为 {@code /swagger-ui.html}（内部再转发静态
 * {@code /swagger-ui/index.html}）。若把 {@code path} 设成 {@code /swagger-ui/index.html}，会与资源映射冲突，
 * 经网关访问易出现 404。</li>
 * </ul>
 * <p>
 * 覆盖方式：在业务工程中显式配置对应属性即可（仅当 property 为 {@code null} 时注入）。
 * </p>
 *
 * @author leiyangjun
 */
public class OpenApiEnvironmentPostProcessor implements EnvironmentPostProcessor {

	/** 与 {@link MapPropertySource#getName()} 一致，供单测断言。 */
	public static final String PROPERTY_SOURCE_NAME = "peach-openapi-springdoc-defaults";

	private static final String SPRINGDOC_CACHE_DISABLED = "springdoc.cache.disabled";

	private static final String SPRINGDOC_SWAGGER_UI_PERSIST_AUTHORIZATION = "springdoc.swagger-ui.persistAuthorization";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> defaults = new HashMap<>(2);
		if (environment.getProperty(SPRINGDOC_CACHE_DISABLED) == null) {
			defaults.put(SPRINGDOC_CACHE_DISABLED, "true");
		}
		if (environment.getProperty(SPRINGDOC_SWAGGER_UI_PERSIST_AUTHORIZATION) == null) {
			defaults.put(SPRINGDOC_SWAGGER_UI_PERSIST_AUTHORIZATION, "true");
		}
		if (defaults.isEmpty()) {
			return;
		}
		environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
	}
}
