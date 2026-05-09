package org.peach.common.mvc.logs.autoconfigure;

import org.peach.common.mvc.logs.web.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Servlet 环境下注册 {@link TraceIdFilter}，将 traceId 写入 MDC 与响应头。
 * 日志级别、文件滚动等由 classpath {@code logback-spring.xml} 决定；可选目录/归档天数仍可通过
 * {@code peach.logging.file.*} 由该 XML 的 {@code springProperty} 读取（无需 Java 配置类）。
 *
 * @author leiyangjun
 */
@AutoConfiguration
public class PeachLoggingAutoConfiguration {

	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public FilterRegistrationBean<TraceIdFilter> peachTraceIdFilter() {
		FilterRegistrationBean<TraceIdFilter> bean = new FilterRegistrationBean<>(new TraceIdFilter());
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		bean.addUrlPatterns("/*");
		bean.setName("peachTraceIdFilter");
		return bean;
	}
}
