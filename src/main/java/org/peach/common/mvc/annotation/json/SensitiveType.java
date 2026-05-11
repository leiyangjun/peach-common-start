package org.peach.common.mvc.annotation.json;

/**
 * JSON 序列化脱敏策略类型（用于 {@link Sensitive}）。
 *
 * @author leiyangjun
 */
public enum SensitiveType {

	/** 中国大陆手机号（11 位数字），示例：138****5678 */
	MOBILE,

	/** 中国大陆居民身份证（18 位），示例：110101********1234 */
	ID_CARD,

	/** 电子邮箱，示例：ab***@example.com */
	EMAIL,

	/** 中文姓名（按 Unicode 判断），2 字常见为「张*」，3 字及以上保留首尾 */
	CHINESE_NAME,

	/** 银行卡号，保留末 4 位，其余分段掩码 */
	BANK_CARD,

	/** 固定电话 / 带区号号码，保留前后片段 */
	FIXED_PHONE,

	/**
	 * 按 {@link Sensitive#prefixKeep()}、{@link Sensitive#suffixKeep()} 保留首尾，中间固定为
	 * {@code *}；总长不足时尽量缩短可见部分。
	 */
	CUSTOM
}
