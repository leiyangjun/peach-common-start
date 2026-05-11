package org.peach.common.utils.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 可选：标注同级排序字段（须实现 {@link Comparable}）。若类型上<strong>没有</strong>本注解，则组树后<strong>不进行</strong>同级排序。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TreeSortField {
}
