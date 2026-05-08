package org.peach.common.openapi.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 为 SpringDoc 提供 Starter 级默认（业务工程无需写 springdoc 段落）：
 * <ul>
 * <li>关闭 OpenAPI 文档缓存，避免先直连再经网关时沿用错误的 {@code servers}；</li>
 * <li>统一 Swagger UI 入口为 {@code /swagger-ui/index.html}，与网关文档门户链接一致（springdoc 自带默认是
 * {@code /swagger-ui.html}，未统一时会出现「有的服务能开 index、有的 404」的错觉）。</li>
 * </ul>
 * <p>
 * 覆盖方式：在业务工程中显式配置对应属性即可。
 * </p>
 */
public class PeachOpenApiEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String SPRINGDOC_CACHE_DISABLED = "springdoc.cache.disabled";

	/** 与 peach-gateway docportal 中 swaggerUrl 保持一致 */
	private static final String SPRINGDOC_SWAGGER_UI_PATH = "springdoc.swagger-ui.path";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> defaults = new HashMap<>(2);
		if (environment.getProperty(SPRINGDOC_CACHE_DISABLED) == null) {
			defaults.put(SPRINGDOC_CACHE_DISABLED, "true");
		}
		if (environment.getProperty(SPRINGDOC_SWAGGER_UI_PATH) == null) {
			defaults.put(SPRINGDOC_SWAGGER_UI_PATH, "/swagger-ui/index.html");
		}
		if (!defaults.isEmpty()) {
			environment.getPropertySources().addLast(new MapPropertySource("peach-openapi-springdoc-defaults", defaults));
		}
	}
}
