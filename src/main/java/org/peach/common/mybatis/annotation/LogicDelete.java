package org.peach.common.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记逻辑删除字段（有效值 / 无效值），与 {@link org.peach.common.mybatis.mapper.CommonSqlProvider} 及各 {@code *SqlProvider} 配套。
 *
 * @author leiyangjun
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LogicDelete {

	/**
	 * 有效状态取值。
	 */
	int valid() default 1;

	/**
	 * 无效（已删除）状态取值。
	 */
	int invalid() default 0;
}

