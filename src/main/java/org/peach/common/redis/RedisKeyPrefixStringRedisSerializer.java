package org.peach.common.redis;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;


/**
 * Redis 字符串 Key 序列化：写入经 {@link RedisKeyBuilder#buildKey(String)} 加前缀，读取时去掉 MODULE-ACTIVE 两段。
 */
public final class RedisKeyPrefixStringRedisSerializer implements RedisSerializer<String> {

	@Override
	@Nullable
	public byte[] serialize(@Nullable String businessKey) {
		if (businessKey == null) {
			return null;
		}
		return RedisKeyBuilder.buildKey(businessKey).getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public String deserialize(@Nullable byte[] fullKeyBytes) {
		if (fullKeyBytes == null || fullKeyBytes.length == 0) {
			return null;
		}
		String full = new String(fullKeyBytes, StandardCharsets.UTF_8);
		return stripModuleActivePrefix(full);
	}

	/**
	 * 去掉前两段（MODULE、ACTIVE）返回业务 ID；段数不足时原样返回。
	 */
	static String stripModuleActivePrefix(String fullKey) {
		int i1 = fullKey.indexOf('-');
		if (i1 < 0) {
			return fullKey;
		}
		int i2 = fullKey.indexOf('-', i1 + 1);
		if (i2 < 0) {
			return fullKey;
		}
		return fullKey.substring(i2 + 1);
	}
}
