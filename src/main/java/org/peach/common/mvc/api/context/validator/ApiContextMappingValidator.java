package org.peach.common.mvc.api.context.validator;

import java.lang.reflect.Method;

import org.peach.common.mvc.api.context.annotation.AdminApi;
import org.peach.common.mvc.api.context.annotation.AppApi;
import org.peach.common.mvc.api.context.annotation.OpenApi;
import org.peach.common.mvc.api.context.properties.ApiContextProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 启动期校验：形态注解仅允许在控制器<strong>类</strong>上；类上互斥多标失败；不支持方法级形态注解。
 *
 * @author leiyangjun
 */
public class ApiContextMappingValidator implements SmartInitializingSingleton {

	private final ApiContextProperties properties;

	private final RequestMappingHandlerMapping requestMappingHandlerMapping;

	public ApiContextMappingValidator(ApiContextProperties properties,
			RequestMappingHandlerMapping requestMappingHandlerMapping) {
		this.properties = properties;
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (HandlerMethod handlerMethod : requestMappingHandlerMapping.getHandlerMethods().values()) {
			Class<?> beanType = ClassUtils.getUserClass(handlerMethod.getBeanType());
			if (!ApiContextValidator.isPrefixedSurfaceController(beanType)) {
				continue;
			}
			Method method = handlerMethod.getMethod();
			if (hasAnySurfaceAnnotation(method)) {
				throw new IllegalStateException(conflictMessage(handlerMethod,
						"形态注解仅支持标注在控制器类上，请勿在处理器方法上使用 @AdminApi/@AppApi/@OpenApi"));
			}
			ApiContextValidator.declaredClassSurfaceOrNull(beanType);
			if (properties.isEnable() && properties.getPath() == null) {
				throw new IllegalStateException("peach.api.context.enable=true 时 path 不能为空");
			}
		}
	}

	private static boolean hasAnySurfaceAnnotation(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, AdminApi.class)
				|| AnnotatedElementUtils.hasAnnotation(method, AppApi.class)
				|| AnnotatedElementUtils.hasAnnotation(method, OpenApi.class);
	}

	private static String conflictMessage(HandlerMethod handlerMethod, String detail) {
		return "Peach API 形态校验失败: " + detail + " — handler=" + handlerMethod;
	}
}
