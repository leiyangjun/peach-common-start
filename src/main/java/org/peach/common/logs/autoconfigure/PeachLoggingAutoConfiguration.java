package org.peach.common.logs.autoconfigure;

import org.peach.common.logs.PeachLoggingProperties;
import org.peach.common.logs.web.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Peach 统一日志：注册 traceId 过滤器；具体输出由 classpath 下 {@code logback-spring.xml} 完成。
 */
@AutoConfiguration
@EnableConfigurationProperties(PeachLoggingProperties.class)
public class PeachLoggingAutoConfiguration {

	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnProperty(prefix = "peach.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnProperty(prefix = "peach.logging.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
	public FilterRegistrationBean<TraceIdFilter> peachTraceIdFilter(PeachLoggingProperties properties) {
		PeachLoggingProperties.Trace trace = properties.getTrace();
		FilterRegistrationBean<TraceIdFilter> bean = new FilterRegistrationBean<>(
				new TraceIdFilter(trace.getHeaderName(), trace.getMdcKey()));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		bean.addUrlPatterns("/*");
		bean.setName("peachTraceIdFilter");
		return bean;
	}
}
