package org.peach.common.mvc.cloud;

import java.util.Locale;

/**
 * 进程内模块编码前缀缓存：仅由 {@link PeachApplicationBootstrapConfiguration} 在配置校验通过后调用
 * {@link #setCachedModule(String)}
 * 写入；{@link org.peach.common.mvc.result.ApiResult}、
 * {@link org.peach.common.mvc.exception.ErrorResult} 通过 {@link #getModule()}
 * 读取以拼接业务 {@code code}。
 */
public final class ModuleCodeCache {

	private static volatile String moduleCode;

	private static volatile String active;

	private ModuleCodeCache() {
	}

	/** @return 已缓存的模块编码前缀；写入前可能为 {@code null} */
	public static String getModule() {
		return moduleCode;
	}

	public static String getActive() {
		return active;
	}

	/**
	 * 由 {@link PeachApplicationBootstrapConfiguration} 独占调用；入参已由该校验保证合法，此处不做重复校验。
	 * {@code null} 表示清除缓存（单测等场景），与 {@link org.peach.common.redis.RedisKeyBuilder} 的 COMM/DEFAULT 回退一致。
	 */
	static void setCachedModule(String moduleCode) {
		ModuleCodeCache.moduleCode = moduleCode == null ? null : moduleCode.toUpperCase(Locale.ROOT);
	}

	/** {@code null} 表示清除缓存（单测等场景）。 */
	static void setActive(String active) {
		ModuleCodeCache.active = active == null ? null : active.toUpperCase(Locale.ROOT);
	}
}
