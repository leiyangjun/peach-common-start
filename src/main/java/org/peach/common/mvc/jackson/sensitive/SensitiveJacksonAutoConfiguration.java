package org.peach.common.mvc.jackson.sensitive;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;

/**
 * 注册脱敏 Jackson {@link JacksonModule}，随 Spring Boot 默认 {@link ObjectMapper} 一并生效。
 *
 * @author leiyangjun
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
public class SensitiveJacksonAutoConfiguration {

	@Bean
	public JacksonModule peachSensitiveJacksonModule() {
		return new SensitiveJacksonModule();
	}
}
