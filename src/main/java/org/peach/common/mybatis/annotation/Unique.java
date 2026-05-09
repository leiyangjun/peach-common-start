package org.peach.common.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「单字段唯一」列：同一实体类上<strong>至多标注一个</strong>字段；多字段联合唯一请用数据库约束 + 其它校验方式，不由本注解描述。
 * <p>
 * {@link org.peach.common.mybatis.mapper.BaseMapper#selectUnique(Object, Class)} /
 * {@link org.peach.common.mybatis.mapper.BaseMapper#selectUniqueValid(Object, Class)} 仅接受<strong>唯一键列对应的一个值</strong>与实体 {@link Class}，
 * 与 {@code checkUnique(uniqueValue, Class, excludeKey)} / {@code checkExist(...)} 等配合使用（单值 + 类型，排除主键可选）。
 * </p>
 *
 * @author leiyangjun
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Unique {
}

