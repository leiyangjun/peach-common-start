package org.peach.common.mvc.autoconfigure;

import org.peach.common.mvc.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet Web 环境下注册 {@link GlobalExceptionHandler}（Starter 自身包默认不被业务扫描覆盖）。
 * <p>
 * API 版本由 Spring Boot 4 的 {@code spring.mvc.apiversion} 提供原生能力；本模块仅在全局异常中补充
 * {@link org.springframework.web.accept.MissingApiVersionException} / {@link org.springframework.web.accept.InvalidApiVersionException}
 * 到 {@link org.peach.common.mvc.result.ApiResult} 的映射说明见 {@link org.peach.common.mvc.exception.GlobalExceptionHandler}。
 * </p>
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RestController.class)
@Import(GlobalExceptionHandler.class)
public class MvcExceptionAutoConfiguration {

	@PostConstruct
	void logConfigurationLoaded() {
		log.info("peach-common-start 自动配置已激活: MvcExceptionAutoConfiguration（全局异常处理）");
	}
}

