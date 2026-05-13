package org.peach.common.mvc.api.context.validator;

import java.util.EnumSet;
import java.util.function.Predicate;

import org.peach.common.mvc.api.context.ApiType;
import org.peach.common.mvc.api.context.annotation.AdminApi;
import org.peach.common.mvc.api.context.annotation.AppApi;
import org.peach.common.mvc.api.context.annotation.OpenApi;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

/**
 * 判断哪些 Spring MVC 控制器参与形态路径前缀与启动期校验，并提供类级形态解析。
 * <p>
 * 规则概要：凡 Spring Web 声明为 {@link Controller}/{@link RestController} 的控制器类型，
 * 且<strong>不</strong>属于框架/文档类包（如 {@code org.springframework.*}、SpringDoc、Springfox）的，
 * 即视为业务「表面」控制器，与业务包名无关。
 * </p>
 *
 * @author leiyangjun
 */
public final class ApiContextValidator {

	private ApiContextValidator() {
	}

	/**
	 * 返回与 {@link org.springframework.web.servlet.config.annotation.PathMatchConfigurer#addPathPrefix(String, Predicate)} 一致的类型谓词。
	 */
	public static Predicate<Class<?>> prefixedSurfaceController() {
		return ApiContextValidator::isPrefixedSurfaceController;
	}

	/**
	 * 若返回 {@code true}，表示该控制器类型属于需加前缀（在开启时）且需做形态注解校验的表面。
	 */
	public static boolean isPrefixedSurfaceController(Class<?> beanType) {
		if (beanType == null) {
			return false;
		}
		Class<?> userClass = ClassUtils.getUserClass(beanType);
		if (!AnnotatedElementUtils.hasAnnotation(userClass, Controller.class)) {
			return false;
		}
		String name = userClass.getName();
		return !isFrameworkOrDocsController(name);
	}

	/**
	 * 框架内置、Actuator、OpenAPI 文档等控制器不参与形态前缀与注解强校验。
	 */
	static boolean isFrameworkOrDocsController(String fullyQualifiedClassName) {
		if (fullyQualifiedClassName == null || fullyQualifiedClassName.isEmpty()) {
			return true;
		}
		if (fullyQualifiedClassName.startsWith("org.springframework.")) {
			return true;
		}
		if (fullyQualifiedClassName.startsWith("org.springdoc.")) {
			return true;
		}
		if (fullyQualifiedClassName.startsWith("springfox.")) {
			return true;
		}
		return false;
	}

	/**
	 * 读取控制器<strong>类</strong>上声明的形态（不支持方法级）；若类上互斥多标则抛异常；若未标注则返回 {@code null}。
	 */
	public static ApiType declaredClassSurfaceOrNull(Class<?> beanType) {
		Class<?> userClass = ClassUtils.getUserClass(beanType);
		EnumSet<ApiType> set = resolveSurfacesOnElement(userClass);
		if (set.size() > 1) {
			throw new IllegalStateException(
					"Peach API 形态冲突: 控制器 " + userClass.getName() + " 的类上存在多个 @AdminApi/@AppApi/@OpenApi");
		}
		return set.isEmpty() ? null : set.iterator().next();
	}

	/**
	 * 类上无形态注解时使用 {@code defaultApiType}（来自 {@code peach.api.context.path}）。
	 */
	public static ApiType effectiveClassSurface(Class<?> beanType, ApiType defaultApiType) {
		if (defaultApiType == null) {
			throw new IllegalStateException("peach.api.context.enable=true 时默认形态 path 不能为空");
		}
		ApiType declared = declaredClassSurfaceOrNull(beanType);
		return declared != null ? declared : defaultApiType;
	}

	private static EnumSet<ApiType> resolveSurfacesOnElement(java.lang.reflect.AnnotatedElement element) {
		EnumSet<ApiType> set = EnumSet.noneOf(ApiType.class);
		if (AnnotatedElementUtils.hasAnnotation(element, AdminApi.class)) {
			set.add(ApiType.ADMIN);
		}
		if (AnnotatedElementUtils.hasAnnotation(element, AppApi.class)) {
			set.add(ApiType.APP);
		}
		if (AnnotatedElementUtils.hasAnnotation(element, OpenApi.class)) {
			set.add(ApiType.OPENAPI);
		}
		return set;
	}
}
