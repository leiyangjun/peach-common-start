package org.peach.common.mvc.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@link PeachNacosBootstrapEnvironmentPostProcessor} 单测。
 */
class PeachNacosBootstrapEnvironmentPostProcessorTest {

	private final PeachNacosBootstrapEnvironmentPostProcessor processor = new PeachNacosBootstrapEnvironmentPostProcessor();

	private static MapPropertySource peachSource(MockEnvironment environment) {
		return (MapPropertySource) environment.getPropertySources()
				.get(PeachNacosBootstrapEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);
	}

	private static Set<String> excludeClasses(MockEnvironment environment) {
		String raw = environment.getProperty(PeachNacosBootstrapEnvironmentPostProcessor.AUTOCONFIGURE_EXCLUDE_KEY);
		if (raw == null || raw.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(raw.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	private void run(MockEnvironment environment) {
		processor.postProcessEnvironment(environment, new SpringApplication());
	}

	@Test
	void discovery_enabled_false_excludes_discovery_autoconfig() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.DISCOVERY_ENABLED_KEY, "false");

		run(environment);

		MapPropertySource source = peachSource(environment);
		assertNotNull(source);
		assertEquals("false", environment.getProperty("spring.cloud.nacos.discovery.enabled"));
		assertEquals("false", environment.getProperty("spring.cloud.discovery.enabled"));
		assertEquals("false", environment.getProperty("spring.cloud.nacos.discovery.register-enabled"));

		Set<String> excludes = excludeClasses(environment);
		assertTrue(excludes.contains(PeachNacosBootstrapEnvironmentPostProcessor.EXCLUDE_NACOS_DISCOVERY));
		assertTrue(excludes.contains(PeachNacosBootstrapEnvironmentPostProcessor.EXCLUDE_NACOS_SERVICE_REGISTRY));
	}

	@Test
	void config_enabled_false_excludes_config_autoconfig() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "false");
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.APPLICATION_NAME_KEY, "demo-service");

		run(environment);

		assertNotNull(peachSource(environment));
		assertEquals("false", environment.getProperty("spring.cloud.nacos.config.enabled"));
		assertEquals("false", environment.getProperty("spring.cloud.nacos.config.import-check.enabled"));
		assertNull(environment.getProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_IMPORT_KEY));

		Set<String> excludes = excludeClasses(environment);
		assertTrue(excludes.contains(PeachNacosBootstrapEnvironmentPostProcessor.EXCLUDE_NACOS_CONFIG));
	}

	@Test
	void both_enabled_true_injects_config_import_without_redundant_enabled_override() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.DISCOVERY_ENABLED_KEY, "true");
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "true");
		environment.setProperty("spring.application.name", "peach-demo");

		run(environment);

		MapPropertySource source = peachSource(environment);
		assertNotNull(source);
		assertTrue(excludeClasses(environment).isEmpty());
		assertNull(source.getProperty(PeachNacosBootstrapEnvironmentPostProcessor.DISCOVERY_ENABLED_KEY));
		assertNull(source.getProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY));
		assertEquals("optional:nacos:peach-demo.yaml?group=DEFAULT_GROUP&refreshEnabled=true",
				environment.getProperty("spring.config.import"));
	}

	@Test
	void discovery_enabled_true_does_not_override_existing_enabled_false() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.DISCOVERY_ENABLED_KEY, "true");
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "false");
		environment.setProperty("spring.application.name", "peach-demo");

		run(environment);

		assertEquals("true", environment.getProperty(PeachNacosBootstrapEnvironmentPostProcessor.DISCOVERY_ENABLED_KEY));
	}

	@Test
	void missing_enabled_injects_config_import_when_app_name_present() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.name", "peach-demo");
		environment.setProperty("spring.profiles.active", "test");

		run(environment);

		assertNotNull(peachSource(environment));
		assertEquals("optional:nacos:peach-demo.yaml?group=DEFAULT_GROUP&refreshEnabled=true",
				environment.getProperty("spring.config.import"));
	}

	@Test
	void bridges_unresolved_namespace_from_active_profile() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "false");
		environment.setProperty("spring.profiles.active", "test");
		environment.setProperty("spring.cloud.nacos.discovery.namespace",
				"${NACOS_NAMESPACE:${spring.profiles.active}}");

		run(environment);

		assertEquals("test", environment.getProperty("spring.cloud.nacos.discovery.namespace"));
	}

	@Test
	void namespace_prefers_nacos_namespace_env() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "false");
		environment.setProperty("NACOS_NAMESPACE", "staging");
		environment.setProperty("spring.profiles.active", "test");
		environment.setProperty("spring.cloud.nacos.discovery.namespace",
				"${NACOS_NAMESPACE:${spring.profiles.active}}");

		run(environment);

		assertEquals("staging", environment.getProperty("spring.cloud.nacos.discovery.namespace"));
	}

	@Test
	void config_enabled_false_does_not_inject_config_import() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "false");
		environment.setProperty("spring.application.name", "peach-demo");

		run(environment);

		assertNull(environment.getProperty("spring.config.import"));
	}

	@Test
	void does_not_duplicate_existing_nacos_import() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.name", "peach-demo");
		environment.setProperty("spring.config.import", "optional:nacos:custom.yaml");

		run(environment);

		assertEquals("optional:nacos:custom.yaml", environment.getProperty("spring.config.import"));
	}

	@Test
	void merges_existing_autoconfigure_exclude() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.AUTOCONFIGURE_EXCLUDE_KEY,
				"com.example.ExistingAutoConfiguration");
		environment.setProperty(PeachNacosBootstrapEnvironmentPostProcessor.CONFIG_ENABLED_KEY, "false");

		run(environment);

		Set<String> excludes = excludeClasses(environment);
		assertTrue(excludes.contains("com.example.ExistingAutoConfiguration"));
		assertTrue(excludes.contains(PeachNacosBootstrapEnvironmentPostProcessor.EXCLUDE_NACOS_CONFIG));
	}

	@Test
	void isExplicitlyDisabled_accepts_false_string() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("test.flag", "false");
		assertTrue(PeachNacosBootstrapEnvironmentPostProcessor.isExplicitlyDisabled(environment, "test.flag"));
	}

	@Test
	void isExplicitlyDisabled_rejects_missing_and_true() {
		MockEnvironment environment = new MockEnvironment();
		assertFalse(PeachNacosBootstrapEnvironmentPostProcessor.isExplicitlyDisabled(environment, "test.flag"));
		environment.setProperty("test.flag", "true");
		assertFalse(PeachNacosBootstrapEnvironmentPostProcessor.isExplicitlyDisabled(environment, "test.flag"));
	}

}
