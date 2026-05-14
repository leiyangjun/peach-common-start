package org.peach.common.redis;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

/**
 * Redis 字符串 Key 序列化：写入时通过 {@link RedisKeyBuilder#buildKey(String)} 加前缀，
 * 读取时去掉 {@code MODULE-ACTIVE-} 两段前缀，还原为业务 ID。
 * <p>
 * 仅适用于「逻辑 Key 为业务尾段」的场景；直连 Lettuce/Jedis 不会经过本序列化器。
 * </p>
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
	 * 去掉前两段（MODULE、ACTIVE），返回业务 ID；格式非法时原样返回，避免误删。
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
