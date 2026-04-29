package org.peach.common.discovery.autoconfigure;

import org.peach.common.discovery.PeachServerServiceDescriptionProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * 将 {@code server.service-description} 同步到 Nacos 实例元数据。
 * <p>
 * 通过 {@link PeachNacosDiscoveryMetadataImportSelector} 仅在 classpath 存在 Nacos 时加载注册逻辑。
 * </p>
 */
@AutoConfiguration
@Import(PeachNacosDiscoveryMetadataImportSelector.class)
@EnableConfigurationProperties(PeachServerServiceDescriptionProperties.class)
public class PeachNacosDiscoveryMetadataAutoConfiguration {
}
