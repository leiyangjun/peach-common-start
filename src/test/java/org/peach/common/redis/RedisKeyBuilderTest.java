package org.peach.common.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.peach.common.mvc.cloud.ModuleCodeCache;

/**
 * {@link RedisKeyBuilder} 键规则与校验单测（通过反射写入 {@link ModuleCodeCache} 静态字段）。
 */
class RedisKeyBuilderTest {

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

	private void givenModuleAndActive(String module, String active) throws Exception {
		invokeSetCachedModule(module);
		invokeSetActive(active);
	}

	@Test
	void buildKey_concatenates_uppercase_module_and_active() throws Exception {
		givenModuleAndActive("comm", "dev");
		assertEquals("COMM-DEV-user123", RedisKeyBuilder.buildKey("user123"));
	}

	@Test
	void blank_module_falls_back_comm() throws Exception {
		givenModuleAndActive("   ", "prod");
		assertEquals("COMM-PROD-x", RedisKeyBuilder.buildKey("x"));
	}

	@Test
	void null_module_falls_back_comm() throws Exception {
		givenModuleAndActive(null, "prod");
		assertEquals("COMM-PROD-y", RedisKeyBuilder.buildKey("y"));
	}

	@Test
	void blank_active_falls_back_default() throws Exception {
		givenModuleAndActive("svc", "  ");
		assertEquals("SVC-DEFAULT-any", RedisKeyBuilder.buildKey("any"));
	}

	@Test
	void null_active_falls_back_default() throws Exception {
		givenModuleAndActive("svc", null);
		assertEquals("SVC-DEFAULT-z", RedisKeyBuilder.buildKey("z"));
	}

	@Test
	void module_uppercases_testmod() throws Exception {
		givenModuleAndActive("testmod", "dev");
		assertEquals("TESTMOD-DEV-k", RedisKeyBuilder.buildKey("k"));
	}

	@Test
	void buildKey_prefix_shape() throws Exception {
		givenModuleAndActive("a", "b");
		assertTrue(RedisKeyBuilder.buildKey("tail").startsWith("A-B-"));
	}

	@Test
	void reject_empty_or_dash_or_colon_in_business_id() throws Exception {
		givenModuleAndActive("COMM", "DEV");
		assertThrows(IllegalArgumentException.class, () -> RedisKeyBuilder.buildKey(""));
		assertThrows(IllegalArgumentException.class, () -> RedisKeyBuilder.buildKey("   "));
		assertThrows(IllegalArgumentException.class, () -> RedisKeyBuilder.buildKey("a-b"));
		assertThrows(IllegalArgumentException.class, () -> RedisKeyBuilder.buildKey("a:b"));
		assertThrows(IllegalArgumentException.class, () -> RedisKeyBuilder.buildKey(null));
	}
}
