package org.peach.common.mvc.autoconfigure;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 注册 Apache HttpClient 5 连接池 Bean，供 {@link org.springframework.http.client.HttpComponentsClientHttpRequestFactory}
 * 与 {@link org.springframework.web.client.RestClient} 复用，减少短连接与线程开销。
 * <p>
 * <b>Bean 名：</b>{@link #PEACH_POOLED_HTTP_CLIENT}。下游可按任务超时再包一层
 * {@link org.springframework.http.client.HttpComponentsClientHttpRequestFactory}（构造参数为本池返回的
 * {@link org.apache.hc.client5.http.impl.classic.CloseableHttpClient}），再
 * {@code setConnectionRequestTimeout}/{@code setReadTimeout}（Spring Framework 7 起 {@link HttpComponentsClientHttpRequestFactory} 不再提供
 * {@code setConnectTimeout}，建立连接超时由连接管理器的 {@link org.apache.hc.client5.http.config.ConnectionConfig} 控制，
 * 见 {@link PeachHttpPoolProperties}），底层仍共享本池。
 * </p>
 *
 * @author leiyangjun
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(CloseableHttpClient.class)
@EnableConfigurationProperties(PeachHttpPoolProperties.class)
public class PeachHttpClientPoolAutoConfiguration {

	/** 与 {@link #peachPooledHttpClient(PeachHttpPoolProperties)} 注册的 Bean 名一致。 */
	public static final String PEACH_POOLED_HTTP_CLIENT = "peachPooledHttpClient";

	@Bean(destroyMethod = "close", name = PEACH_POOLED_HTTP_CLIENT)
	@ConditionalOnMissingBean(name = PEACH_POOLED_HTTP_CLIENT)
	public CloseableHttpClient peachPooledHttpClient(PeachHttpPoolProperties props) {
		// HttpClient5：建立连接超时归属 ConnectionConfig，不再使用 RequestConfig.Builder#setConnectTimeout（已过时）
		ConnectionConfig defaultConnectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.ofMilliseconds(props.getConnectTimeoutMs()))
				.build();
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(defaultConnectionConfig)
				.setMaxConnTotal(props.getMaxTotal())
				.setMaxConnPerRoute(props.getMaxPerRoute())
				.build();

		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(props.getConnectionRequestTimeoutMs()))
				.setResponseTimeout(Timeout.ofMilliseconds(props.getResponseTimeoutMs()))
				.build();

		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(defaultRequestConfig)
				.evictIdleConnections(TimeValue.ofSeconds(30))
				.evictExpiredConnections()
				.build();
	}
}
