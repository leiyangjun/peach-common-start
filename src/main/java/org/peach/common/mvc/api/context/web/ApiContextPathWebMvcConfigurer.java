package org.peach.common.mvc.api.context.web;

import org.peach.common.mvc.api.context.ApiType;
import org.peach.common.mvc.api.context.properties.ApiContextProperties;
import org.peach.common.mvc.api.context.validator.ApiContextValidator;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 在开启配置时，为符合条件的业务控制器注册形态路径前缀：{@code /admin}、{@code /app}、{@code /openapi} 之一。
 * <p>
 * 形态注解<strong>仅支持标注在控制器类</strong>上；类未标注时使用 {@code peach.api.context.path} 作为默认形态。
 * </p>
 *
 * @author leiyangjun
 */
public class ApiContextPathWebMvcConfigurer implements WebMvcConfigurer {

	private final ApiContextProperties properties;

	public ApiContextPathWebMvcConfigurer(ApiContextProperties properties) {
		this.properties = properties;
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		ApiType defaultApiType = properties.getPath();
		if (defaultApiType == null) {
			throw new IllegalStateException("peach.api.context.enable=true 时 path 不能为空，请配置 peach.api.context.path 为 admin、app 或 openapi");
		}
		for (ApiType apiType : ApiType.values()) {
			final ApiType target = apiType;
			configurer.addPathPrefix("/" + target.getApiType(), candidate -> {
				Class<?> userClass = ClassUtils.getUserClass(candidate);
				if (!ApiContextValidator.isPrefixedSurfaceController(userClass)) {
					return false;
				}
				return ApiContextValidator.effectiveClassSurface(userClass, defaultApiType) == target;
			});
		}
	}
}
