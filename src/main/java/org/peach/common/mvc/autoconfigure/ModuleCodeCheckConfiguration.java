package org.peach.common.mvc.autoconfigure;

import org.peach.common.mvc.result.ApiResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 校验并初始化 {@code spring.application.module-code}（四位字符串），供 {@link ApiResult} 拼接业务码使用。
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

	public ModuleCodeCheckConfiguration(SpringApplicationModuleProperties props) {
		log.info("peach-common-start 自动配置已激活: ModuleCodeCheckConfiguration（校验 spring.application.module-code）");
		String mc = props.getModuleCode();
		if (mc == null || mc.isBlank()) {
			log.error("启动失败：缺少必要配置 spring.application.module-code（四位模块编码）");
			throw new IllegalStateException(
					"未配置 spring.application.module-code（四位模块编码），请在 application.yml 中设置，例如：spring.application.module-code: B001");
		}
		log.info("检测到模块编码配置 spring.application.module-code={}", mc);
		ApiResult.setModuleCode(mc);
		log.info("模块编码校验通过，ApiResult 业务码前缀初始化完成");
	}
}

