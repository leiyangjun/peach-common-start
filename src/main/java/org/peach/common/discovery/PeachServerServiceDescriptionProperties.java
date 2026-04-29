package org.peach.common.discovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 与 {@code server.port} 同级：{@code server.service-description}，可选；
 * 存在时将写入 Nacos 实例元数据 {@link PeachDiscoveryMetadataKeys#SERVICE_DESCRIPTION}，供网关 Swagger 门户等展示。
 */
@ConfigurationProperties(prefix = "server")
public class PeachServerServiceDescriptionProperties {

	private String serviceDescription;

	public String getServiceDescription() {
		return serviceDescription;
	}

	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}
}
