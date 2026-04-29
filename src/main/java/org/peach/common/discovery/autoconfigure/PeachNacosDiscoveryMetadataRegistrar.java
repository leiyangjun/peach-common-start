package org.peach.common.discovery.autoconfigure;

import org.peach.common.discovery.PeachDiscoveryMetadataKeys;
import org.peach.common.discovery.PeachServerServiceDescriptionProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;

/**
 * 在 Nacos 注册前把 {@code server.service-description} 写入 {@link NacosDiscoveryProperties#getMetadata()}。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(NacosDiscoveryProperties.class)
@AutoConfigureAfter(NacosDiscoveryAutoConfiguration.class)
public class PeachNacosDiscoveryMetadataRegistrar {

	public PeachNacosDiscoveryMetadataRegistrar(NacosDiscoveryProperties nacos, PeachServerServiceDescriptionProperties server) {
		if (StringUtils.hasText(server.getServiceDescription())) {
			nacos.getMetadata().put(PeachDiscoveryMetadataKeys.SERVICE_DESCRIPTION, server.getServiceDescription().trim());
		}
	}
}
