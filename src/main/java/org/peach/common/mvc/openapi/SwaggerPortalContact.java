package org.peach.common.mvc.openapi;

/**
 * 网关 API 文档门户在 OpenAPI {@code info.contact} 中的展示常量（名称、可选静态 URL）；
 * 经网关访问时的绝对链接一般由 {@link SwaggerOpenApiCustomizer} 动态填充。
 */
public final class SwaggerPortalContact {

	private SwaggerPortalContact() {
	}

	/** 联系人名称 */
	public static final String NAME = "回到网关API列表";

	/**
	 * 静态联系人链接（可选）。
	 * <p>
	 * 一般留空：经网关访问时由 {@link SwaggerOpenApiCustomizer} 根据当前请求的
	 * {@code X-Forwarded-Proto} / {@code X-Forwarded-Host}（或 {@code Host}）拼出绝对地址
	 * {@code 网关根/index.html}，无需配置网关 URL；直连调试时通常不展示链接。
	 * </p>
	 */
	public static final String URL = "";
}
