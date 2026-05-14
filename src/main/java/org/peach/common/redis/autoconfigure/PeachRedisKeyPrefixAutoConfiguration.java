package org.peach.common.redis.autoconfigure;

import org.peach.common.redis.RedisTemplateKeyPrefixConfigurer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 仅在类路径存在 Spring Data Redis 时注册 {@link RedisTemplateKeyPrefixConfigurer}，
 * 避免 starter 将 {@code spring-boot-starter-data-redis} 标为 optional 时，消费端未引入 Redis 却在组件扫描中加载到
 * 依赖 {@code RedisTemplate} 的字节码而导致启动失败。
 * <p>
 * 条件使用 {@link ConditionalOnClass#name()} 而非 {@code RedisTemplate.class}，避免本配置类自身在 JVM
 * 解析阶段即触发对 optional 类型的链接。
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
public class PeachRedisKeyPrefixAutoConfiguration {

	/**
	 * 为 StringRedisTemplate / 符合条件的 RedisTemplate 装配统一 Key 前缀序列化器。
	 */
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	static BeanPostProcessor redisTemplateKeyPrefixConfigurer() {
		return new RedisTemplateKeyPrefixConfigurer();
	}
}
