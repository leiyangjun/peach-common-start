package org.peach.common.mvc.api.context.autoconfigure;

import org.peach.common.mvc.api.context.properties.ApiContextProperties;
import org.peach.common.mvc.api.context.validator.ApiContextMappingValidator;
import org.peach.common.mvc.api.context.web.ApiContextPathWebMvcConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet Web MVC 下注册 Peach API 全局路径前缀（可关闭）与启动期形态注解校验。
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(ApiContextProperties.class)
public class ApiContextAutoConfiguration {

	@PostConstruct
	void logConfigurationLoaded() {
		log.info("peach-common-start 自动配置已激活: ApiContextAutoConfiguration（peach.api.context 路径前缀与形态校验）");
	}

	/**
	 * 开启 {@code peach.api.context.enabled} 时，按形态（默认 path + 类上注解）为业务控制器注册 /admin、/app、/openapi 路径前缀。
	 */
	@Bean
	@ConditionalOnProperty(prefix = "peach.api.context", name = "enabled", havingValue = "true", matchIfMissing = true)
	public WebMvcConfigurer apiContextPathWebMvcConfigurer(ApiContextProperties properties) {
		return new ApiContextPathWebMvcConfigurer(properties);
	}

	/**
	 * 单例初始化完成后校验「表面」控制器上的 @AdminApi / @AppApi / @OpenApi 仅出现在类上等规则。
	 */
	@Bean
	public ApiContextMappingValidator apiContextMappingValidator(ApiContextProperties properties,
			RequestMappingHandlerMapping requestMappingHandlerMapping) {
		return new ApiContextMappingValidator(properties, requestMappingHandlerMapping);
	}
}
