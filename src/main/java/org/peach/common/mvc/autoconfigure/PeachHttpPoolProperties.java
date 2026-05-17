package org.peach.common.mvc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link PeachHttpClientPoolAutoConfiguration} 使用的连接池与默认超时（毫秒）。
 * <p>
 * 配置前缀：{@code peach.http.pool.*}
 * </p>
 *
 * @author leiyangjun
 */
@ConfigurationProperties(prefix = "peach.http.pool")
public class PeachHttpPoolProperties {

	/** 连接池最大连接总数 */
	private int maxTotal = 200;

	/** 每个路由（目标 host:port）最大连接数 */
	private int maxPerRoute = 50;

	/** 建立 TCP 连接超时（毫秒） */
	private int connectTimeoutMs = 5_000;

	/** 从池中取连接等待超时（毫秒） */
	private int connectionRequestTimeoutMs = 5_000;

	/** 响应读取超时（毫秒） */
	private int responseTimeoutMs = 30_000;

	public int getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}

	public int getMaxPerRoute() {
		return maxPerRoute;
	}

	public void setMaxPerRoute(int maxPerRoute) {
		this.maxPerRoute = maxPerRoute;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public int getConnectionRequestTimeoutMs() {
		return connectionRequestTimeoutMs;
	}

	public void setConnectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
		this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
	}

	public int getResponseTimeoutMs() {
		return responseTimeoutMs;
	}

	public void setResponseTimeoutMs(int responseTimeoutMs) {
		this.responseTimeoutMs = responseTimeoutMs;
	}
}
