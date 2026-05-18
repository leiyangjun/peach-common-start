package org.peach.common.mvc.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.peach.common.mvc.autoconfigure.SpringApplicationModuleProperties;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@link PeachApplicationBootstrapConfiguration} 启动校验单测。
 */
class PeachApplicationBootstrapConfigurationTest {

	private static final String APPLICATION_NAME = "peach-common-service";

	@BeforeEach
	@AfterEach
	void resetModuleCodeCache() throws Exception {
		invokeSetCachedModule(null);
		invokeSetActive(null);
	}

	private static void invokeSetCachedModule(String moduleCode) throws Exception {
		Method m = ModuleCodeCache.class.getDeclaredMethod("setCachedModule", String.class);
		m.setAccessible(true);
		m.invoke(null, moduleCode);
	}

	private static void invokeSetActive(String active) throws Exception {
		Method m = ModuleCodeCache.class.getDeclaredMethod("setActive", String.class);
		m.setAccessible(true);
		m.invoke(null, active);
	}

	private static SpringApplicationModuleProperties validProps() {
		SpringApplicationModuleProperties props = new SpringApplicationModuleProperties();
		props.setModuleCode("COMM");
		return props;
	}

	private static MockEnvironment environmentWithNameAndProfile(String profile) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.name", APPLICATION_NAME);
		if (profile != null) {
			environment.setProperty("spring.profiles.active", profile);
		}
		return environment;
	}

	@Test
	void uses_first_active_profile_when_available() {
		MockEnvironment environment = environmentWithNameAndProfile("dev");
		environment.setActiveProfiles("prod");

		new PeachApplicationBootstrapConfiguration(validProps(), environment);

		assertEquals("PROD", ModuleCodeCache.getActive());
		assertEquals("COMM", ModuleCodeCache.getModule());
	}

	@Test
	void falls_back_to_spring_profiles_active_property_when_active_profiles_empty() {
		MockEnvironment environment = environmentWithNameAndProfile("staging");

		new PeachApplicationBootstrapConfiguration(validProps(), environment);

		assertEquals("STAGING", ModuleCodeCache.getActive());
		assertEquals("COMM", ModuleCodeCache.getModule());
	}

	@Test
	void fails_when_active_profiles_and_property_both_missing() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.name", APPLICATION_NAME);

		assertThrows(IllegalStateException.class,
				() -> new PeachApplicationBootstrapConfiguration(validProps(), environment));
	}

	@Test
	void rejects_invalid_module_code() {
		MockEnvironment environment = environmentWithNameAndProfile("dev");
		SpringApplicationModuleProperties props = new SpringApplicationModuleProperties();
		props.setModuleCode("1234");

		assertThrows(IllegalStateException.class,
				() -> new PeachApplicationBootstrapConfiguration(props, environment));
	}

	@Test
	void fails_when_application_name_blank() {
		MockEnvironment environment = environmentWithNameAndProfile("dev");
		environment.setProperty("spring.application.name", "   ");

		assertThrows(IllegalStateException.class,
				() -> new PeachApplicationBootstrapConfiguration(validProps(), environment));
	}

	@Test
	void fails_when_application_name_missing() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "dev");

		assertThrows(IllegalStateException.class,
				() -> new PeachApplicationBootstrapConfiguration(validProps(), environment));
	}

}
