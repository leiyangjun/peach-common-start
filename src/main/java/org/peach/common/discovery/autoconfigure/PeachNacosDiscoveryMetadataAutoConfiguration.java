package org.peach.common.discovery.autoconfigure;

import org.peach.common.discovery.PeachDiscoveryMetadataKeys;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;

/**
 * 将 {@code server.description} 同步到 Nacos 实例元数据（键 {@link PeachDiscoveryMetadataKeys#DESCRIPTION}）。
 * <p>
 * <b>为何用 {@link BeanPostProcessor}：</b>此前在独立 {@code @Configuration} 构造器里写入 metadata，
 * 但该配置类可能早于 {@link NacosDiscoveryAutoConfiguration} 处理，导致
 * {@code @ConditionalOnBean(NacosDiscoveryProperties.class)} 不成立，整段注册逻辑被跳过，Nacos 中始终没有说明元数据。
 * 在 {@link NacosDiscoveryProperties} 初始化完成后追加写入，可保证顺序正确。
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(NacosDiscoveryProperties.class)
@AutoConfigureAfter(NacosDiscoveryAutoConfiguration.class)
public class PeachNacosDiscoveryMetadataAutoConfiguration {

	@Bean
	public BeanPostProcessor peachNacosServerDescriptionMetadataBeanPostProcessor(Environment environment) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (!(bean instanceof NacosDiscoveryProperties nacos)) {
					return bean;
				}
				String desc = environment.getProperty("server.description");
				if (StringUtils.hasText(desc)) {
					nacos.getMetadata().put(PeachDiscoveryMetadataKeys.DESCRIPTION, desc.trim());
				}
				return bean;
			}
		};
	}
}
