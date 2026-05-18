package org.peach.common.mvc.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@link ModuleCodeCheckConfiguration} 模块编码与环境标识解析单测。
 */
class ModuleCodeCheckConfigurationTest {

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

	@Test
	void uses_first_active_profile_when_available() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		environment.setProperty("spring.profiles.active", "dev");

		new ModuleCodeCheckConfiguration(validProps(), environment);

		assertEquals("PROD", ModuleCodeCache.getActive());
		assertEquals("COMM", ModuleCodeCache.getModule());
	}

	@Test
	void falls_back_to_spring_profiles_active_property_when_active_profiles_empty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "staging");

		new ModuleCodeCheckConfiguration(validProps(), environment);

		assertEquals("STAGING", ModuleCodeCache.getActive());
		assertEquals("COMM", ModuleCodeCache.getModule());
	}

	@Test
	void fails_when_active_profiles_and_property_both_missing() {
		MockEnvironment environment = new MockEnvironment();

		assertThrows(IllegalStateException.class, () -> new ModuleCodeCheckConfiguration(validProps(), environment));
	}

	@Test
	void rejects_invalid_module_code() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "dev");
		SpringApplicationModuleProperties props = new SpringApplicationModuleProperties();
		props.setModuleCode("1234");

		assertThrows(IllegalStateException.class, () -> new ModuleCodeCheckConfiguration(props, environment));
	}

}
