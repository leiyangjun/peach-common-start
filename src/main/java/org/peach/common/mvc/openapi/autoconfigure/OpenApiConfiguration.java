package org.peach.common.mvc.openapi.autoconfigure;

import org.peach.common.mvc.openapi.SwaggerOpenApiCustomizer;
import org.peach.common.mvc.openapi.SwaggerPortalContact;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

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

	/** OpenAPI 全局 HTTP Bearer JWT 方案名（与 {@link SecurityRequirement} 引用一致）。 */
	public static final String BEARER_AUTH_SCHEME = "bearerAuth";

	private static final String DEFAULT_API_VERSION = "1.0.0";

	private static final String BEARER_AUTH_DESCRIPTION =
			"登录接口返回的 accessToken；Swagger UI 中仅填写 JWT 字符串即可（会自动添加 Bearer 前缀）";

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
		SecurityScheme bearerAuth = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
				.bearerFormat("JWT").description(BEARER_AUTH_DESCRIPTION);
		return new OpenAPI().info(info)
				.components(new Components().addSecuritySchemes(BEARER_AUTH_SCHEME, bearerAuth))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
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
