package org.peach.common.mvc.autoconfigure;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 注册<strong>服务间调用</strong>用的 {@link RestClient.Builder}：带 {@link LoadBalanced}，可对
 * {@code http://注册中心中的服务名/路径} 做客户端负载均衡；底层使用 {@link HttpComponentsClientHttpRequestFactory}
 * 绑定 {@link PeachHttpClientPoolAutoConfiguration} 提供的<strong>连接池</strong> {@link CloseableHttpClient}。
 * <p>
 * {@link Scope} 为 <b>prototype</b>：请配合 {@link org.springframework.beans.factory.ObjectProvider#getObject()}
 * 每次请求新建 Builder 再 {@link RestClient.Builder#build()}，避免多线程任务下共享可变 Builder。
 * </p>
 * <p>
 * <b>Bean 名：</b>{@link #PEACH_INTER_SERVICE_REST_CLIENT_BUILDER}（与 {@code @Qualifier} 字符串一致）。
 * </p>
 * <p>
 * <b>下游用法示例</b>（业务类中注入 {@link org.springframework.beans.factory.ObjectProvider}；每次远程调用再
 * {@code getObject().requestFactory(...).build()}；{@code uri} 使用 {@code http://服务注册名/路径}；超时示例使用
 * 池化 {@link CloseableHttpClient} 再包一层 {@link HttpComponentsClientHttpRequestFactory}）：
 * </p>
 *
 * <pre>
 * {@code @Autowired}
 * {@code @Qualifier(InterServiceRestClientAutoConfiguration.PEACH_INTER_SERVICE_REST_CLIENT_BUILDER)}
 * {@code private ObjectProvider<RestClient.Builder> interServiceRestClientBuilder;}
 * </pre>
 *
 * <pre>
 * {@code @Autowired @Qualifier(PeachHttpClientPoolAutoConfiguration.PEACH_POOLED_HTTP_CLIENT)}
 * {@code private CloseableHttpClient peachPooledHttpClient;}
 * {@code // ...}
 * {@code HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(peachPooledHttpClient);}
 * {@code rf.setConnectionRequestTimeout(Duration.ofMillis(5000));}
 * {@code rf.setReadTimeout(Duration.ofMillis(10_000));}
 * {@code RestClient client = interServiceRestClientBuilder.getObject().requestFactory(rf).build();}
 * {@code String body = client.get()}
 * {@code     .uri("http://peach-common-service/admin/xxx")}
 * {@code     .retrieve()}
 * {@code     .body(String.class);}
 * </pre>
 *
 * <p>
 * 需自行 {@code import}：{@code org.springframework.beans.factory.ObjectProvider}、
 * {@code org.springframework.beans.factory.annotation.Qualifier}、
 * {@code org.springframework.http.client.HttpComponentsClientHttpRequestFactory}、{@code java.time.Duration}、
 * {@code org.apache.hc.client5.http.impl.classic.CloseableHttpClient}、{@code org.springframework.web.client.RestClient}。
 * </p>
 * <p>
 * 若业务需统一超时/连接池，可自声明<strong>同名</strong> Bean 覆盖。
 * </p>
 *
 * @author leiyangjun
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(LoadBalancerAutoConfiguration.class)
@ConditionalOnBean(name = PeachHttpClientPoolAutoConfiguration.PEACH_POOLED_HTTP_CLIENT)
@AutoConfigureAfter({ PeachHttpClientPoolAutoConfiguration.class, LoadBalancerAutoConfiguration.class })
public class InterServiceRestClientAutoConfiguration {

	/** 与 {@link #peachInterServiceRestClientBuilder(CloseableHttpClient)} 注册的 Bean 名一致，供 {@code @Qualifier} 引用。 */
	public static final String PEACH_INTER_SERVICE_REST_CLIENT_BUILDER = "peachInterServiceRestClientBuilder";

	@Bean(PEACH_INTER_SERVICE_REST_CLIENT_BUILDER)
	@LoadBalanced
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean(name = PEACH_INTER_SERVICE_REST_CLIENT_BUILDER)
	public RestClient.Builder peachInterServiceRestClientBuilder(
			@Qualifier(PeachHttpClientPoolAutoConfiguration.PEACH_POOLED_HTTP_CLIENT) CloseableHttpClient peachPooledHttpClient) {
		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(peachPooledHttpClient);
		return RestClient.builder().requestFactory(rf);
	}
}
