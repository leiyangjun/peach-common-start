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
 * <ul>
 * <li>关闭 OpenAPI 文档缓存，避免先直连再经网关时沿用错误的 {@code servers}；</li>
 * <li>不覆盖 {@code springdoc.swagger-ui.path}：SpringDoc 官方入口为 {@code /swagger-ui.html}（内部再转发静态
 * {@code /swagger-ui/index.html}）。若把 {@code path} 设成 {@code /swagger-ui/index.html}，会与资源映射冲突，
 * 经网关访问易出现 404。</li>
 * </ul>
 * <p>
 * 覆盖方式：在业务工程中显式配置对应属性即可。
 * </p>
 *
 * @author leiyangjun
 */
public class OpenApiEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String SPRINGDOC_CACHE_DISABLED = "springdoc.cache.disabled";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.getProperty(SPRINGDOC_CACHE_DISABLED) != null) {
			return;
		}
		Map<String, Object> defaults = new HashMap<>(1);
		defaults.put(SPRINGDOC_CACHE_DISABLED, "true");
		environment.getPropertySources().addLast(new MapPropertySource("peach-openapi-springdoc-defaults", defaults));
	}
}
