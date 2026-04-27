package org.peach.common.mvc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 扩展 {@code spring.application} 下的自定义项：模块编码（四位）。
 * <p>
 * 配置示例：{@code spring.application.module-code: AB12}（或 YAML 中等价写法 {@code moduleCode}）。
 * </p>
 *
 * @author leiyangjun
 */
@ConfigurationProperties(prefix = "spring.application")
public class SpringApplicationModuleProperties {

	/**
	 * 四位模块编码，全服务唯一前缀，参与 {@link org.peach.common.mvc.result.ApiResult} 的 {@code code} 第一段。
	 */
	private String moduleCode;

	public String getModuleCode() {
		return moduleCode;
	}

	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}
}

