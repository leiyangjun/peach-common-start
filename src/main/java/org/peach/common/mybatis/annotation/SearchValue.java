package org.peach.common.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记参与关键字模糊查询的字段（与入参 {@code searchValue} 组合，见 {@code likeSelectBase}）。
 *
 * @author leiyangjun
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SearchValue {
}

