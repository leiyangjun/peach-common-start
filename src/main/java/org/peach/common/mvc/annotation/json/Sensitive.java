package org.peach.common.mvc.annotation.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要在 JSON 序列化时脱敏的字段（通常为 {@link String}）。
 * <p>
 * 由 Starter 注册的 Jackson {@link tools.jackson.databind.JacksonModule} 在写出 JSON 前替换为掩码后的字符串，
 * 一般用于返回给前端的 VO/DTO，<strong>不影响</strong>数据库持久化的实体本身。
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Sensitive {

	/** 脱敏策略 */
	SensitiveType value();

	/**
	 * 仅 {@link SensitiveType#CUSTOM}：保留首部明文长度（≥0）。
	 */
	int prefixKeep() default 0;

	/**
	 * 仅 {@link SensitiveType#CUSTOM}：保留尾部明文长度（≥0）。
	 */
	int suffixKeep() default 0;
}
