package org.peach.common.mvc.api.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明该控制器属于「应用端」对外形态（{@link org.peach.common.mvc.api.context.ApiType#APP}），仅可标注在<strong>控制器类</strong>上。
 * <p>
 * 未标注时整类使用配置 {@code peach.api.context.path} 的默认形态对应的前缀。
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AppApi {
}
