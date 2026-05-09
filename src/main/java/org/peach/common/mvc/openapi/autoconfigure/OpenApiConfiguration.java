package org.peach.common.mvc.openapi.autoconfigure;

import org.peach.common.mvc.openapi.SwaggerOpenApiCustomizer;
import org.peach.common.mvc.openapi.SwaggerPortalContact;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/**
 * SpringDoc 全局 {@link OpenAPI} 与 {@link OpenApiCustomizer}。
 * <p>
 * 使用普通 {@link Configuration} 并由 {@link SwaggerAutoConfiguration} {@code @Import}，语义上等价于在业务工程中自建
 * {@code @Configuration} 类并声明 Bean（仍随 starter 生效，无需组件扫描到本类）。
 * </p>
 *
 * @author leiyangjun
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	private static final String DEFAULT_API_VERSION = "1.0.0";

	@Bean
	@Primary
	public OpenAPI peachOpenApi(
			@Value("${spring.application.name:application}") String applicationName,
			@Value("${server.description:}") String description,
			@Value("${peach.openapi.version:1.0.0}") String apiVersion) {
		Info info = new Info().title(applicationName).version(resolveVersion(apiVersion));
		if (StringUtils.hasText(description)) {
			info.setDescription(description.trim());
		}
		Contact contact = new Contact().name(SwaggerPortalContact.NAME);
		if (StringUtils.hasText(SwaggerPortalContact.URL)) {
			contact.url(SwaggerPortalContact.URL.trim());
		}
		info.setContact(contact);
		return new OpenAPI().info(info);
	}

	private static String resolveVersion(String configured) {
		return StringUtils.hasText(configured) ? configured.trim() : DEFAULT_API_VERSION;
	}

	@Bean
	public OpenApiCustomizer swaggerOpenApiCustomizer(
			@Value("${spring.application.name:application}") String applicationName,
			@Value("${server.port:8080}") int serverPort) {
		return new SwaggerOpenApiCustomizer(applicationName, serverPort);
	}
}
