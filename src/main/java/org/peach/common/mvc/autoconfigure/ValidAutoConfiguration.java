package org.peach.common.mvc.autoconfigure;

import org.peach.common.mvc.validation.ValidAspect;
import org.peach.common.mvc.validation.ValidationUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.validation.SmartValidator;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * 注册 {@link ValidAspect}、{@link ValidationUtils}；需存在 Bean Validation 与 AOP 能力。
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnBean({ SmartValidator.class, Validator.class })
@EnableAspectJAutoProxy
public class ValidAutoConfiguration {

	@PostConstruct
	void logConfigurationLoaded() {
		log.info("peach-common-start 自动配置已激活: ValidAutoConfiguration（ValidAspect / ValidationUtils）");
	}

	@Bean
	@ConditionalOnMissingBean
	public ValidationUtils validationUtils(Validator jakartaValidator) {
		return new ValidationUtils(jakartaValidator);
	}

	@Bean
	@ConditionalOnMissingBean
	public ValidAspect validAspect(SmartValidator smartValidator) {
		return new ValidAspect(smartValidator);
	}
}

