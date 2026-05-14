package org.peach.common.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.peach.common.mvc.autoconfigure.ModuleCodeCache;

/**
 * {@link RedisKeyPrefixStringRedisSerializer} 前缀剥离与序列化往返单测。
 */
class RedisKeyPrefixStringRedisSerializerTest {

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

	@BeforeEach
	@AfterEach
	void resetModuleCodeCache() throws Exception {
		invokeSetCachedModule(null);
		invokeSetActive(null);
	}

	@Test
	void stripPrefix_two_segments() {
		assertEquals("tail", RedisKeyPrefixStringRedisSerializer.stripModuleActivePrefix("A-B-tail"));
	}

	@Test
	void stripPrefix_malformed_returns_as_is() {
		assertEquals("onlyone", RedisKeyPrefixStringRedisSerializer.stripModuleActivePrefix("onlyone"));
	}

	@Test
	void serialize_then_deserialize_roundTrip() throws Exception {
		invokeSetCachedModule("svc");
		invokeSetActive("dev");
		RedisKeyPrefixStringRedisSerializer ser = new RedisKeyPrefixStringRedisSerializer();
		assertEquals("user1", ser.deserialize(ser.serialize("user1")));
	}
}
