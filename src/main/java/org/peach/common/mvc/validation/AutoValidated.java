package org.peach.common.mvc.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.RestController;

/**
 * 启用 {@link ValidAspect}：在 <b>未</b> 对 {@code @RequestBody} 标注 {@code @Valid} / {@code @Validated}
 * 时，仍由切面在进入 Controller 方法前做 Bean Validation，效果与「先校验再执行业务」一致。
 * <p>
 * 应标注在 {@link RestController} 类上（与 {@code @Within(RestController)} 切点配合）。
 * </p>
 *
 * @author leiyangjun
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoValidated {

	/**
	 * 是否对带 {@link org.springframework.web.bind.annotation.RequestBody} 的参数做校验。
	 */
	boolean requestBody() default true;
}

