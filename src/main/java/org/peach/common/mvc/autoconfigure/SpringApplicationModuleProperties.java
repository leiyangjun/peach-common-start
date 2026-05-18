package org.peach.common.mvc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 扩展 {@code spring.application} 下的自定义项：模块编码（四位）、环境标识等。
 * <p>
 * 配置示例：{@code spring.application.module-code: AB12}（或 YAML 中等价写法
 * {@code moduleCode}）。校验通过后由 {@link org.peach.common.mvc.cloud.PeachApplicationBootstrapConfiguration}
 * 经 {@link org.peach.common.mvc.cloud.ModuleCodeCache} 写入缓存（模块段大写），供
 * {@link org.peach.common.mvc.result.ApiResult}、{@link org.peach.common.redis.RedisKeyBuilder}
 * 等使用。
 * </p>
 *
 * @author leiyangjun
 */
@Data
@ConfigurationProperties(prefix = "spring.application")
public class SpringApplicationModuleProperties {

	/**
	 * 四位模块编码，全服务唯一前缀；写入 {@link org.peach.common.mvc.cloud.ModuleCodeCache} 后参与
	 * {@link org.peach.common.mvc.result.ApiResult} 等 {@code code} 第一段拼接。
	 */
	private String moduleCode;

}
