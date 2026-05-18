package org.peach.common.mvc.cloud;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Starter 约定：以 {@code spring.cloud.nacos.discovery.enabled} / {@code config.enabled} 控制 Nacos
 * 注册发现与配置中心（默认 {@code true}，见 {@code application.yml}）。
 * <p>
 * 下游设 {@code enabled=false} 时关闭 SCA 注册/远程配置，并追加
 * {@code spring.autoconfigure.exclude}，真正关闭注册与配置拉取。
 * </p>
 * <p>
 * 配置中心开启且下游未声明 nacos 相关 {@code spring.config.import} 时，自动注入
 * {@code optional:nacos:${spring.application.name}.yaml}（group 与 starter {@code application.yml} 一致）。
 * </p>
 * <p>
 * 实现 {@link org.springframework.boot.EnvironmentPostProcessor}（Boot 4 推荐接口；
 * {@code org.springframework.boot.env.EnvironmentPostProcessor} 自 4.0 起已弃用）。
 * </p>
 */
public class PeachNacosBootstrapEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/** 注入到 Environment 的属性源名称，便于测试与排查。 */
	public static final String PROPERTY_SOURCE_NAME = "peachNacosBootstrap";

	static final String APPLICATION_NAME_KEY = "spring.application.name";
	static final String CONFIG_IMPORT_KEY = "spring.config.import";
	static final String CONFIG_GROUP_KEY = "spring.cloud.nacos.config.group";
	static final String NACOS_CONFIG_GROUP_ENV = "NACOS_CONFIG_GROUP";
	static final String NACOS_GROUP_ENV = "NACOS_GROUP";

	static final String DISCOVERY_ENABLED_KEY = "spring.cloud.nacos.discovery.enabled";
	static final String DISCOVERY_REGISTER_ENABLED_KEY = "spring.cloud.nacos.discovery.register-enabled";
	static final String SPRING_CLOUD_DISCOVERY_ENABLED_KEY = "spring.cloud.discovery.enabled";
	static final String CONFIG_ENABLED_KEY = "spring.cloud.nacos.config.enabled";
	static final String DISCOVERY_NAMESPACE_KEY = "spring.cloud.nacos.discovery.namespace";
	static final String CONFIG_NAMESPACE_KEY = "spring.cloud.nacos.config.namespace";
	static final String NACOS_NAMESPACE_ENV = "NACOS_NAMESPACE";
	static final String PROFILES_ACTIVE_KEY = "spring.profiles.active";
	static final String AUTOCONFIGURE_EXCLUDE_KEY = "spring.autoconfigure.exclude";

	static final String EXCLUDE_NACOS_DISCOVERY = "com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration";
	static final String EXCLUDE_NACOS_SERVICE_REGISTRY = "com.alibaba.cloud.nacos.registry.NacosServiceRegistryAutoConfiguration";
	static final String EXCLUDE_NACOS_CONFIG = "com.alibaba.cloud.nacos.NacosConfigSpringCloudAutoConfiguration";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> overrides = new LinkedHashMap<>();
		Set<String> excludes = new LinkedHashSet<>();

		if (isExplicitlyDisabled(environment, DISCOVERY_ENABLED_KEY)) {
			overrides.put(DISCOVERY_REGISTER_ENABLED_KEY, false);
			overrides.put(SPRING_CLOUD_DISCOVERY_ENABLED_KEY, false);
			overrides.put(DISCOVERY_ENABLED_KEY, false);
			excludes.add(EXCLUDE_NACOS_DISCOVERY);
			excludes.add(EXCLUDE_NACOS_SERVICE_REGISTRY);
		}
		else {
			bridgeNamespaceIfNeeded(environment, overrides);
		}

		if (isExplicitlyDisabled(environment, CONFIG_ENABLED_KEY)) {
			overrides.put(CONFIG_ENABLED_KEY, false);
			overrides.put("spring.cloud.nacos.config.import-check.enabled", false);
			excludes.add(EXCLUDE_NACOS_CONFIG);
		}
		else {
			appendConfigImportIfNeeded(environment, overrides);
			bridgeNamespaceIfNeeded(environment, overrides);
		}

		if (!excludes.isEmpty()) {
			overrides.put(AUTOCONFIGURE_EXCLUDE_KEY, mergeExcludes(environment, excludes));
		}

		if (overrides.isEmpty()) {
			return;
		}
		environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
	}

	/**
	 * 当 namespace 未解析（含 {@code ${}} 占位）或为空时，用 {@code NACOS_NAMESPACE} 或 {@code spring.profiles.active} 回填。
	 */
	static void bridgeNamespaceIfNeeded(Environment environment, Map<String, Object> overrides) {
		String resolved = resolveNamespace(environment);
		if (!StringUtils.hasText(resolved)) {
			return;
		}
		if (needsNamespaceBridge(environment.getProperty(DISCOVERY_NAMESPACE_KEY))) {
			overrides.put(DISCOVERY_NAMESPACE_KEY, resolved);
		}
		if (needsNamespaceBridge(environment.getProperty(CONFIG_NAMESPACE_KEY))) {
			overrides.put(CONFIG_NAMESPACE_KEY, resolved);
		}
	}

	static boolean needsNamespaceBridge(String namespace) {
		if (!StringUtils.hasText(namespace)) {
			return true;
		}
		return namespace.contains("${");
	}

	static String resolveNamespace(Environment environment) {
		String fromEnv = environment.getProperty(NACOS_NAMESPACE_ENV);
		if (StringUtils.hasText(fromEnv)) {
			return fromEnv.trim();
		}
		String profile = environment.getProperty(PROFILES_ACTIVE_KEY);
		if (StringUtils.hasText(profile)) {
			return profile.split(",")[0].trim();
		}
		if (environment instanceof ConfigurableEnvironment configurable) {
			String[] active = configurable.getActiveProfiles();
			if (active.length > 0 && StringUtils.hasText(active[0])) {
				return active[0].trim();
			}
		}
		return null;
	}

	static void appendConfigImportIfNeeded(Environment environment, Map<String, Object> overrides) {
		if (containsNacosImport(environment)) {
			return;
		}
		String appName = environment.getProperty(APPLICATION_NAME_KEY);
		if (!StringUtils.hasText(appName)) {
			return;
		}
		String group = resolveConfigGroup(environment);
		String dataId = appName.trim() + ".yaml";
		String importEntry = "optional:nacos:" + dataId + "?group=" + group + "&refreshEnabled=true";
		String existing = environment.getProperty(CONFIG_IMPORT_KEY);
		if (!StringUtils.hasText(existing)) {
			overrides.put(CONFIG_IMPORT_KEY, importEntry);
		}
		else {
			overrides.put(CONFIG_IMPORT_KEY, existing.trim() + "," + importEntry);
		}
	}

	static String resolveConfigGroup(Environment environment) {
		String group = environment.getProperty(NACOS_CONFIG_GROUP_ENV);
		if (StringUtils.hasText(group)) {
			return group.trim();
		}
		group = environment.getProperty(NACOS_GROUP_ENV);
		if (StringUtils.hasText(group)) {
			return group.trim();
		}
		group = environment.getProperty(CONFIG_GROUP_KEY);
		if (StringUtils.hasText(group)) {
			return group.trim();
		}
		return "DEFAULT_GROUP";
	}

	static boolean containsNacosImport(Environment environment) {
		if (environment.containsProperty(CONFIG_IMPORT_KEY)) {
			String value = environment.getProperty(CONFIG_IMPORT_KEY);
			if (containsIgnoreCase(value, "nacos:")) {
				return true;
			}
		}
		for (int i = 0; i < 32; i++) {
			String indexed = environment.getProperty(CONFIG_IMPORT_KEY + "[" + i + "]");
			if (indexed == null) {
				break;
			}
			if (containsIgnoreCase(indexed, "nacos:")) {
				return true;
			}
		}
		return false;
	}

	/** 忽略大小写子串匹配（替代已弃用的 Commons {@code StringUtils.containsIgnoreCase}）。 */
	private static boolean containsIgnoreCase(String str, String search) {
		if (str == null || search == null) {
			return false;
		}
		return str.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
	}

	/**
	 * 仅当属性显式为 {@code false} 时视为关闭（未配置或 {@code true} 不处理）。
	 */
	static boolean isExplicitlyDisabled(Environment environment, String key) {
		if (!environment.containsProperty(key)) {
			return false;
		}
		return Boolean.FALSE.equals(environment.getProperty(key, Boolean.class));
	}

	static String mergeExcludes(Environment environment, Collection<String> additions) {
		Set<String> merged = new LinkedHashSet<>();
		String existing = environment.getProperty(AUTOCONFIGURE_EXCLUDE_KEY);
		if (existing != null && !existing.isBlank()) {
			Arrays.stream(existing.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.forEach(merged::add);
		}
		merged.addAll(additions);
		return String.join(",", merged);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 20;
	}
}
