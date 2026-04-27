package org.peach.common.mybatis.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 创作日期：2026-04-26，作者：Codex
 * 读写分离数据源配置。关闭时沿用 spring.datasource；开启时由本配置构建写库/读库与路由数据源。
 */
@ConfigurationProperties(prefix = "spring.datasource.rw")
public class ReadWriteDataSourceProperties {

	/** 是否启用读写分离路由。 */
	private boolean enabled = false;

	/** 写库（主库）配置。 */
	private final Node write = new Node();

	/** 读库（从库）配置。 */
	private final Node read = new Node();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Node getWrite() {
		return write;
	}

	public Node getRead() {
		return read;
	}

	public String resolveReadUrl() {
		return StringUtils.defaultIfBlank(read.getUrl(), write.getUrl());
	}

	public String resolveReadUsername() {
		return StringUtils.defaultIfBlank(read.getUsername(), write.getUsername());
	}

	public String resolveReadPassword() {
		return StringUtils.defaultIfBlank(read.getPassword(), write.getPassword());
	}

	public String resolveReadDriverClassName() {
		return StringUtils.defaultIfBlank(read.getDriverClassName(), write.getDriverClassName());
	}

	public static class Node {

		private String url;
		private String username;
		private String password;
		private String driverClassName;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getDriverClassName() {
			return driverClassName;
		}

		public void setDriverClassName(String driverClassName) {
			this.driverClassName = driverClassName;
		}
	}
}

