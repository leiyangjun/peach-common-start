package org.peach.common.utils.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注树节点主键字段；参与 {@link org.peach.common.utils.TreeUtil#tree(java.util.List, Class)} 的类型每类须恰好一个。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TreeId {
}
