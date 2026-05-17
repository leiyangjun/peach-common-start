package org.peach.common.redis.autoconfigure;

import org.peach.common.redis.RedisTemplateKeyPrefixConfigurer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;


/**
 * 类路径存在 Spring Data Redis 时注册 {@link RedisTemplateKeyPrefixConfigurer}；
 * 条件使用 {@link ConditionalOnClass#name()}，以免未引入 Redis 时链接 {@code RedisTemplate}。
 *
 * @author leiyangjun
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
