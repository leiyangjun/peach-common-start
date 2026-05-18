package org.peach.common.redis;

import org.peach.common.mvc.cloud.ModuleCodeCache;

/**
 * Redis 键前缀构建：{@code {MODULE}-{ACTIVE}-{业务ID}}，段间 {@code '-'}。
 * <p>
 * {@code MODULE}、{@code ACTIVE} 均取自 {@link ModuleCodeCache#getModule()}、
 * {@link ModuleCodeCache#getActive()}（由
 * {@link org.peach.common.mvc.cloud.PeachApplicationBootstrapConfiguration}
 * 在校验通过后写入）；此处仅做 trim + 大写规范化。未写入前可能为 {@code null}，则模块段回退 {@code COMM}、环境段回退
 * {@code DEFAULT}。
 * </p>
 * <p>
 * 仅接受业务尾段 {@code businessId}：去空白后非空，且不得包含 {@code ':'}、{@code '-'}。
 * </p>
 */
public final class RedisKeyBuilder {

	private RedisKeyBuilder() {
	}

	/**
	 * 拼接完整 Redis 键。
	 *
	 * @param businessId 业务尾段
	 * @return {@code MODULE-ACTIVE-businessId}
	 */
	public static String buildKey(String businessId) {
		if (businessId == null) {
			throw new IllegalArgumentException("businessId 不能为空");
		}
		String tail = businessId.trim();
		if (tail.isEmpty()) {
			throw new IllegalArgumentException("businessId 不能为空");
		}
		if (tail.indexOf(':') >= 0 || tail.indexOf('-') >= 0) {
			throw new IllegalArgumentException("businessId 不得包含 ':' 或 '-'");
		}
		String module = normalizeSegment(ModuleCodeCache.getModule(), "COMM");
		String active = normalizeSegment(ModuleCodeCache.getActive(), "DEFAULT");
		return module + "-" + active + "-" + tail;
	}

	private static String normalizeSegment(String raw, String fallbackUpper) {
		if (raw == null) {
			return fallbackUpper;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return fallbackUpper;
		}
		return t.toUpperCase();
	}
}
