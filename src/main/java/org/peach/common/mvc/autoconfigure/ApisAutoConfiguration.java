package org.peach.common.mvc.autoconfigure;

import org.peach.common.mvc.web.ApisController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet Web 环境下注册 {@link ApisController}，使引用本 Starter 的服务默认具备 {@code GET /apis} 查询能力。
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RestController.class)
@Import(ApisController.class)
public class ApisAutoConfiguration {

	@PostConstruct
	void logConfigurationLoaded() {
		log.info("peach-common-start 自动配置已激活: ApisAutoConfiguration（注册 /apis 映射查询）");
	}
}
