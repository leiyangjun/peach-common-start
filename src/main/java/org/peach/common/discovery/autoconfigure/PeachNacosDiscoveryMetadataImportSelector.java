package org.peach.common.discovery.autoconfigure;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 仅在存在 Nacos Discovery 类时导入 {@link PeachNacosDiscoveryMetadataRegistrar}。
 */
public class PeachNacosDiscoveryMetadataImportSelector implements ImportSelector {

	private static final String NACOS_DISCOVERY_PROPERTIES = "com.alibaba.cloud.nacos.NacosDiscoveryProperties";

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		try {
			Class.forName(NACOS_DISCOVERY_PROPERTIES, false, PeachNacosDiscoveryMetadataImportSelector.class.getClassLoader());
			return new String[] { PeachNacosDiscoveryMetadataRegistrar.class.getName() };
		}
		catch (ClassNotFoundException | LinkageError e) {
			return new String[0];
		}
	}
}
