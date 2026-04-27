package org.peach.common.mvc;

import org.jspecify.annotations.NonNull;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 在非 Spring 托管类（如工具类、静态方法、三方回调、旧代码）中获取 Bean 的便捷入口。
 * <p>
 * 由 {@link org.peach.common.mvc.autoconfigure.SpringBeanUtilAutoConfiguration} 注册为单例并完成
 * {@link ApplicationContextAware} 回调；优先仍应使用构造器 / 字段注入，本类仅作兜底。
 * </p>
 * <p>
 * 注意：容器尚未刷新完成前调用会抛出 {@link IllegalStateException}。
 * </p>
 *
 * @author leiyangjun
 */
public class SpringBeanUtil implements ApplicationContextAware {

	private static volatile ApplicationContext context;

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		SpringBeanUtil.context = applicationContext;
	}

	/**
	 * 获取当前应用上下文（已就绪时）。
	 */
	public static ApplicationContext getApplicationContext() {
		return requireContext();
	}

	private static ApplicationContext requireContext() {
		ApplicationContext ctx = context;
		if (ctx == null) {
			throw new IllegalStateException("ApplicationContext 尚未就绪，请避免在 Spring 容器初始化完成前调用 SpringBeanUtil");
		}
		return ctx;
	}

	/**
	 * 按类型获取唯一 Bean（与 {@link ApplicationContext#getBean(Class)} 语义一致）。
	 */
	public static <T> T getBean(Class<T> clazz) {
		return requireContext().getBean(clazz);
	}

	/**
	 * 按名称获取 Bean。
	 */
	public static Object getBean(String name) {
		return requireContext().getBean(name);
	}

	/**
	 * 按名称与类型获取 Bean。
	 */
	public static <T> T getBean(String name, Class<T> clazz) {
		return requireContext().getBean(name, clazz);
	}
}

