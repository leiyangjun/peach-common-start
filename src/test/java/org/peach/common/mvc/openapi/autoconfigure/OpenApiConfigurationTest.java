package org.peach.common.mvc.openapi.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * {@link OpenApiConfiguration} 全局 Bearer JWT 单测。
 */
class OpenApiConfigurationTest {

	private final OpenApiConfiguration configuration = new OpenApiConfiguration();

	@Test
	void peachOpenApi_registers_global_bearer_jwt() {
		OpenAPI openApi = configuration.peachOpenApi("peach-demo", "demo api", "2.0.0");

		assertNotNull(openApi.getComponents());
		SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.BEARER_AUTH_SCHEME);
		assertNotNull(scheme);
		assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
		assertEquals("bearer", scheme.getScheme());
		assertEquals("JWT", scheme.getBearerFormat());
		assertTrue(scheme.getDescription().contains("accessToken"));

		assertNotNull(openApi.getSecurity());
		assertEquals(1, openApi.getSecurity().size());
		assertTrue(openApi.getSecurity().getFirst().containsKey(OpenApiConfiguration.BEARER_AUTH_SCHEME));
	}

}
