package org.peach.common.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体主键字段，供 {@link org.peach.common.mybatis.mapper.BaseSqlProvider} 生成 SQL 时使用。
 * <p>
 * 主键为空且需自动生成时：{@link String} / {@link CharSequence} 使用 22 位 URL 安全随机串（见 {@link org.peach.common.utils.IdUtil#shortId22()}）；
 * {@code long}/{@link Long}、{@code int}/{@link Integer} 等整型使用雪花 ID（见 {@link org.peach.common.utils.IdUtil#nextId()}）。
 * </p>
 *
 * @author leiyangjun
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ID {

	/**
	 * 是否为数据库自增主键（为 true 且 {@link #sequenceTag()} 为空时，插入语句可省略主键列）。
	 */
	boolean isSequence() default false;

	/**
	 * 非空时作为主键列的 SQL 片段（如序列函数）。
	 */
	String sequenceTag() default "";

	/**
	 * 为 true 时强制使用雪花算法数值主键（否则仍按字段类型推断：仅 {@code long}/{@link Long} 走雪花）。
	 */
	boolean isSnowflakeHash() default false;
}

