package org.peach.common.mvc.api.context;

/**
 * 业务 HTTP 接口的「对外形态」分类，与全局路径前缀 {@code /admin}、{@code /app}、{@code /openapi} 一一对应。
 *
 * @author leiyangjun
 */
public enum ApiType {

	/**
	 * 管理端形态。
	 */
	ADMIN("admin"),

	/**
	 * 应用端形态。
	 */
	APP("app"),

	/**
	 * 开放 API 形态。
	 */
	OPENAPI("openapi");

	private final String apiType;

	ApiType(String apiType) {
		this.apiType = apiType;
	}

	/**
	 * 配置中的路径段（不含首尾斜杠），用于拼出 {@code "/" + apiType} 形式的全局前缀。
	 */
	public String getApiType() {
		return apiType;
	}
}
