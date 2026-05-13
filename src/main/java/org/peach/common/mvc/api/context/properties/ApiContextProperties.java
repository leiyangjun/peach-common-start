package org.peach.common.mvc.api.context.properties;

import org.peach.common.mvc.api.context.ApiType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Peach 业务 API 全局路径前缀与形态标注相关配置，绑定前缀 {@code peach.api.context}。
 * <p>
 * 当 {@link #enable} 为 {@code true} 时，未在<strong>控制器类</strong>上标注 {@code @AdminApi}/{@code @AppApi}/{@code @OpenApi} 时，整类使用默认 {@link #path} 对应的前缀；
 * 类上显式标注时以注解为准（不支持方法级形态注解）。{@code enable=false} 时不注册路径前缀。
 * </p>
 *
 * @author leiyangjun
 */
@ConfigurationProperties(prefix = "peach.api.context")
public class ApiContextProperties {

	/**
	 * 是否启用全局路径前缀（{@code /admin}、{@code /app}、{@code /openapi} 之一）。
	 */
	private boolean enable = true;

	/**
	 * 与全局前缀对应的路径段；仅在 {@link #enable} 为 {@code true} 时生效。默认 {@link ApiType#ADMIN}。
	 * <p>
	 * {@code enable=false} 时允许省略该配置项，不做「必须非空」校验。
	 * </p>
	 */
	private ApiType path = ApiType.ADMIN;

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public ApiType getPath() {
		return path;
	}

	public void setPath(ApiType path) {
		this.path = path;
	}
}
