package org.peach.common.mvc.cloud;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;

/**
 * 将配置 {@code server.description} 的值同步到 Nacos 实例元数据，键名固定为 {@link #SERVER_DESCRIPTION}。
 * <p>
 * <b>为何使用 {@link BeanPostProcessor}：</b>若在独立 {@code @Configuration} 构造器里写入 metadata，
 * 该配置类可能早于 {@link NacosDiscoveryAutoConfiguration} 处理，导致
 * {@code @ConditionalOnBean(NacosDiscoveryProperties.class)} 不成立而跳过注册；在
 * {@link NacosDiscoveryProperties} 初始化完成后追加写入可保证顺序。
 * </p>
 *
 * @author leiyangjun
 */
@AutoConfiguration
@ConditionalOnClass(NacosDiscoveryProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.nacos.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(NacosDiscoveryAutoConfiguration.class)
public class NacosMetadataAutoConfiguration {

	/** 与 Spring 配置项同名；Nacos 实例 metadata 的键与取值来源均使用此固定字符串。 */
	private static final String SERVER_DESCRIPTION = "server.description";

	/**
	 * 在 Nacos discovery 属性 Bean 初始化后，将 {@link #SERVER_DESCRIPTION} 配置写入同键的实例 metadata。
	 */
	@Bean
	public BeanPostProcessor nacosMetadataBean(Environment environment) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (!(bean instanceof NacosDiscoveryProperties nacos)) {
					return bean;
				}
				String desc = environment.getProperty(SERVER_DESCRIPTION);
				if (StringUtils.hasText(desc)) {
					nacos.getMetadata().put(SERVER_DESCRIPTION, desc.trim());
				}
				return bean;
			}
		};
	}
}
