package org.peach.common.redis;

import org.peach.common.mvc.autoconfigure.ModuleCodeCache;

/**
 * Redis 键前缀构建：{@code {MODULE}-{ACTIVE}-{业务ID}}，段间 {@code '-'}。
 * <p>
 * {@code MODULE}、{@code ACTIVE} 均取自 {@link ModuleCodeCache#getModule()}、
 * {@link ModuleCodeCache#getActive()}（由
 * {@link org.peach.common.mvc.autoconfigure.ModuleCodeCheckConfiguration}
 * 在校验通过后写入）；此处仅做 trim + 大写规范化。未写入前可能为 {@code null}，则模块段回退 {@code COMM}、环境段回退
 * {@code DEFAULT}。
 * </p>
 * <p>
 * 仅接受业务尾段 {@code businessId}：去空白后非空，且不得包含 {@code ':'}、{@code '-'}。
 * </p>
 *
 * @author leiyangjun
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
		return ModuleCodeCache.getModule().toUpperCase() + "-" + ModuleCodeCache.getActive().toUpperCase() + "-"
				+ businessId.trim();
	}
}
