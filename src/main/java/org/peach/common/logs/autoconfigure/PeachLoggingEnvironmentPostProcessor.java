package org.peach.common.logs.autoconfigure;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
/**
 * 按 Spring Profile 注入日志默认级别（低优先级 MapPropertySource，业务配置可覆盖）。
 * <ul>
 * <li>生产：{@code pro}、{@code produce}、{@code product} → root 默认 WARN</li>
 * <li>测试：{@code test} → root 默认 WARN（MyBatis SQL 见 starter 中非生产 profile 的 StdOutImpl，不依赖本处包级别）</li>
 * <li>其余（含未激活 profile、{@code dev} 等）→ root 默认 INFO；{@code peach.logging.dev.debug-packages} 默认
 * DEBUG；并默认开启 MyBatis / JDBC 相关包的 DEBUG（便于本地看 SQL，可被业务 {@code logging.level.*} 覆盖）</li>
 * </ul>
 */
public class PeachLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	public static final String PROPERTY_SOURCE_NAME = "peachLoggingDefaultLevels";

	/** 开发环境默认 SQL 追踪相关 logger（低优先级，仅当未显式配置时生效）。 */
	private static final List<String> DEFAULT_DEV_SQL_DEBUG_PACKAGES = List.of("org.apache.ibatis", "org.mybatis",
			"java.sql");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MutablePropertySources sources = environment.getPropertySources();
		if (sources.contains(PROPERTY_SOURCE_NAME)) {
			return;
		}
		String[] active = environment.getActiveProfiles();
		boolean prodOrTest = Arrays.stream(active).anyMatch(PeachLoggingEnvironmentPostProcessor::isProdOrTestProfile);
		Map<String, Object> defaults = new LinkedHashMap<>();
		if (prodOrTest) {
			putIfAbsent(environment, defaults, "logging.level.root", "WARN");
		}
		else {
			putIfAbsent(environment, defaults, "logging.level.root", "INFO");
			List<String> packages = resolveDebugPackages(environment);
			for (String pkg : packages) {
				if (pkg == null || pkg.isBlank()) {
					continue;
				}
				String key = "logging.level." + pkg.trim();
				putIfAbsent(environment, defaults, key, "DEBUG");
			}
			for (String pkg : DEFAULT_DEV_SQL_DEBUG_PACKAGES) {
				putIfAbsent(environment, defaults, "logging.level." + pkg, "DEBUG");
			}
		}
		if (!defaults.isEmpty()) {
			sources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
		}
	}

	private static void putIfAbsent(ConfigurableEnvironment env, Map<String, Object> map, String key, Object value) {
		if (env.getProperty(key) == null) {
			map.put(key, value);
		}
	}

	private static List<String> resolveDebugPackages(ConfigurableEnvironment environment) {
		List<String> bound = Binder.get(environment)
				.bind("peach.logging.dev.debug-packages", Bindable.listOf(String.class))
				.orElse(List.of("org.peach"));
		if (bound.isEmpty()) {
			return List.of("org.peach");
		}
		return bound;
	}

	private static boolean isProdOrTestProfile(String profile) {
		if (profile == null || profile.isBlank()) {
			return false;
		}
		String p = profile.trim();
		return p.equalsIgnoreCase("pro") || p.equalsIgnoreCase("produce") || p.equalsIgnoreCase("product")
				|| p.equalsIgnoreCase("test");
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}
}
