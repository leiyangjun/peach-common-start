package org.peach.common.mvc.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.result.ApiResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 校验 {@code spring.application.module-code}（须非空且恰好四位）；通过后调用
 * {@link ModuleCodeCache#setCachedModule(String)}，使
 * {@link ModuleCodeCache#getModule()} 与
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

	public ModuleCodeCheckConfiguration(SpringApplicationModuleProperties props) {
		String regex = "^[A-Z][A-Z0-9]{4}$";
		log.info("peach-common-start 自动配置已激活: ModuleCodeCheckConfiguration（校验 spring.application.module-code）");
		String moduleCode = props.getModuleCode();
		if (StringUtils.isBlank(props.getActive()) || StringUtils.isBlank(moduleCode)
				|| StringUtils.length(moduleCode) != 4 || moduleCode.matches(regex)) {
			log.error("启动失败：未正确配置 spring.application.module-code或active（四位模块编码--模块代码由四位字母加数字组成且首位为字母）");
			throw new IllegalStateException("启动失败：未正确配置 spring.application.module-code（四位模块编码--模块代码由四位字母加数字组成且首位为字母）");
		}
		ModuleCodeCache.setCachedModule(moduleCode.trim());
		ModuleCodeCache.setActive(props.getActive());
		log.info("检测到模块编码配置 spring.application.module-code={}", moduleCode);
	}
}
