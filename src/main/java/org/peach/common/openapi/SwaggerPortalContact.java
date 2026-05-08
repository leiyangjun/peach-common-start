package org.peach.common.openapi;

/**
 * 网关 API 文档门户：Swagger UI 顶部「联系人」展示（与 OpenAPI {@code info.contact} 一致）。
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
