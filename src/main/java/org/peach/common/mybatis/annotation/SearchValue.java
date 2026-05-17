package org.peach.common.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记参与关键字模糊查询的字段（与 {@link org.peach.common.mybatis.model.vo.SearchVO#getSearchValue()} 组合，见
 * {@link org.peach.common.mybatis.mapper.BaseMapper#likeSelectBase}）。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SearchValue {
}

