package org.peach.common.mvc.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 在未显式配置时注入 Jackson 全局序列化策略：{@code null} 字段不参与 JSON 输出（与
 * {@code @JsonInclude(JsonInclude.Include.NON_NULL)} 等价）。
 * <p>
 * 通过 {@code spring.jackson.default-property-inclusion=non_null} 注入默认值；
 * 已在 {@code application.yml} 中配置时不覆盖。
 * </p>
 *
 * @author leiyangjun
 */
public class JacksonEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

	public static final String PROPERTY_SOURCE_NAME = "peachJacksonSerializationDefaults";

	private static final String INCLUSION_KEY = "spring.jackson.default-property-inclusion";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
			return;
		}
		if (environment.getProperty(INCLUSION_KEY) != null) {
			return;
		}
		Map<String, Object> defaults = new HashMap<>(1);
		defaults.put(INCLUSION_KEY, "non_null");
		environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 20;
	}
}
