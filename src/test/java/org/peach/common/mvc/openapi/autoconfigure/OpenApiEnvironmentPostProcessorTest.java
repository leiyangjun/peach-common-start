package org.peach.common.mvc.openapi.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@link OpenApiEnvironmentPostProcessor} 单测。
 */
class OpenApiEnvironmentPostProcessorTest {

	private final OpenApiEnvironmentPostProcessor processor = new OpenApiEnvironmentPostProcessor();

	private static MapPropertySource peachSource(MockEnvironment environment) {
		return (MapPropertySource) environment.getPropertySources()
				.get(OpenApiEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);
	}

	private void run(MockEnvironment environment) {
		processor.postProcessEnvironment(environment, new SpringApplication());
	}

	@Test
	void injects_springdoc_defaults_when_unconfigured() {
		MockEnvironment environment = new MockEnvironment();

		run(environment);

		MapPropertySource source = peachSource(environment);
		assertNotNull(source);
		assertEquals("true", environment.getProperty("springdoc.cache.disabled"));
		assertEquals("true", environment.getProperty("springdoc.swagger-ui.persistAuthorization"));
	}

	@Test
	void does_not_override_existing_cache_disabled() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("springdoc.cache.disabled", "false");

		run(environment);

		MapPropertySource source = peachSource(environment);
		assertNotNull(source);
		assertEquals("false", environment.getProperty("springdoc.cache.disabled"));
		assertEquals("true", environment.getProperty("springdoc.swagger-ui.persistAuthorization"));
		assertNull(source.getProperty("springdoc.cache.disabled"));
	}

	@Test
	void does_not_override_existing_persist_authorization() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("springdoc.swagger-ui.persistAuthorization", "false");

		run(environment);

		MapPropertySource source = peachSource(environment);
		assertNotNull(source);
		assertEquals("true", environment.getProperty("springdoc.cache.disabled"));
		assertEquals("false", environment.getProperty("springdoc.swagger-ui.persistAuthorization"));
		assertNull(source.getProperty("springdoc.swagger-ui.persistAuthorization"));
	}

	@Test
	void skips_property_source_when_both_explicitly_configured() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("springdoc.cache.disabled", "false");
		environment.setProperty("springdoc.swagger-ui.persistAuthorization", "false");

		run(environment);

		assertNull(peachSource(environment));
	}

}
