package org.peach.common.redis;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * 在容器初始化完成后，为 {@link StringRedisTemplate} 以及「Key 已为 {@link StringRedisSerializer}」的
 * {@link RedisTemplate} 安装 {@link RedisKeyPrefixStringRedisSerializer}，使经 Template 写入的 Key 自动带统一前缀。
 * <p>
 * 已为本序列化器或 null 的 KeySerializer 不会重复覆盖。
 * </p>
 * <p>
 * 由 {@link org.peach.common.redis.autoconfigure.PeachRedisKeyPrefixAutoConfiguration} 在存在
 * {@link RedisTemplate} 时注册为 {@link BeanPostProcessor}，勿再使用组件扫描注解，以免无 Redis 依赖时类加载失败。
 * </p>
 *
 * @author leiyangjun
 */
public class RedisTemplateKeyPrefixConfigurer implements BeanPostProcessor {

	private static final RedisKeyPrefixStringRedisSerializer PREFIX_SERIALIZER = new RedisKeyPrefixStringRedisSerializer();

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof StringRedisTemplate srt) {
			applyIfNeeded(srt);
			return bean;
		}
		if (bean instanceof RedisTemplate<?, ?> rt) {
			if (shouldApplyGeneric(rt)) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				RedisTemplate raw = (RedisTemplate) rt;
				applyIfNeeded(raw);
			}
		}
		return bean;
	}

	private static boolean shouldApplyGeneric(RedisTemplate<?, ?> rt) {
		return rt.getKeySerializer() instanceof StringRedisSerializer;
	}

	private static void applyIfNeeded(RedisTemplate<?, ?> rt) {
		if (rt.getKeySerializer() instanceof RedisKeyPrefixStringRedisSerializer) {
			return;
		}
		rt.setKeySerializer(PREFIX_SERIALIZER);
		rt.setHashKeySerializer(PREFIX_SERIALIZER);
	}
}
