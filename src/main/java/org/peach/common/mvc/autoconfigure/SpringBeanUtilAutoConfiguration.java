package org.peach.common.mvc.autoconfigure;

import org.peach.common.mvc.SpringBeanUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 注册 {@link SpringBeanUtil}，便于非注入场景解析 Bean。
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ApplicationContext.class)
public class SpringBeanUtilAutoConfiguration {

	@PostConstruct
	void logConfigurationLoaded() {
		log.info("peach-common-start 自动配置已激活: SpringBeanUtilAutoConfiguration");
	}

	@Bean
	public SpringBeanUtil springBeanUtil() {
		return new SpringBeanUtil();
	}
}

