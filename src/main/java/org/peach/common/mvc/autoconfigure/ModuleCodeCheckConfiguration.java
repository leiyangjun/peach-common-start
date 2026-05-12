package org.peach.common.mvc.autoconfigure;

import org.peach.common.mvc.result.ApiResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * 校验 {@code spring.application.module-code}（须非空且恰好四位）；通过后调用
 * {@link ModuleCodeCache#setCachedModule(String)}，使
 * {@link ModuleCodeCache#get()} 与
 * {@link ApiResult}、{@link org.peach.common.mvc.exception.ErrorResult} 拼接业务码一致。
 * <p>
 * 若校验失败会抛出运行时异常，直接中断 Spring Boot 启动流程（不会进入 Web 层统一异常处理）。
 * </p>
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SpringApplicationModuleProperties.class)
public class ModuleCodeCheckConfiguration {

	public ModuleCodeCheckConfiguration(SpringApplicationModuleProperties props, Environment environment) {
		log.info("peach-common-start 自动配置已激活: ModuleCodeCheckConfiguration（校验 spring.application.module-code）");
		String mc = props.getModuleCode();
		if (mc == null || mc.isBlank()) {
			log.error("启动失败：缺少必要配置 spring.application.module-code（四位模块编码）");
			throw new IllegalStateException(
					"未配置 spring.application.module-code（四位模块编码），请在 application.yml 中设置，例如：spring.application.module-code: B001");
		}
		if (mc.length() != 4) {
			log.error("启动失败：spring.application.module-code 须为恰好四位，当前长度={}", mc.length());
			throw new IllegalStateException("spring.application.module-code 须为恰好四位字符，当前长度=" + mc.length());
		}
		ModuleCodeCache.setCachedModule(mc);
		log.info("检测到模块编码配置 spring.application.module-code={}", mc);
	}
}
