package org.peach.common.utils;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;

/**
 * 基于 Spring {@link BeanUtils} 的属性拷贝工具，提供常用静态入口（如 {@link #copy(Object, Class)}）。
 * <p>
 * <strong>关于「继承」：</strong>{@code org.springframework.beans.BeanUtils} 为工具抽象类且构造器为
 * {@code private}，按 Java 语言规则<strong>不能被继承</strong>。本类通过<strong>委托</strong>其静态方法达到封装目的，
 * 行为与 Spring 一致。
 * </p>
 */
public final class BeanUtil {

	private BeanUtil() {
	}

	/**
	 * 将 {@code source} 的同名可写属性拷贝到目标类型的新实例（浅拷贝，规则同 {@link BeanUtils#copyProperties(Object, Object)}）。
	 *
	 * @param source      源对象，为 {@code null} 时返回 {@code null}
	 * @param targetClass 目标类型，须具备可访问的无参构造
	 * @return 新实例；{@code source == null} 时为 {@code null}
	 */
	@Nullable
	public static <T> T copy(@Nullable Object source, Class<T> targetClass) {
		if (source == null) {
			return null;
		}
		Objects.requireNonNull(targetClass, "targetClass");
		try {
			T target = targetClass.getDeclaredConstructor().newInstance();
			BeanUtils.copyProperties(source, target);
			return target;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("无法实例化目标类型: " + targetClass.getName(), ex);
		}
	}

	/**
	 * 属性拷贝到已有目标对象（委托 {@link BeanUtils#copyProperties(Object, Object)}）。
	 */
	public static void copyProperties(Object source, Object target) throws BeansException {
		BeanUtils.copyProperties(source, target);
	}

	/**
	 * 属性拷贝，可忽略指定属性名（委托 {@link BeanUtils#copyProperties(Object, Object, String...)}）。
	 */
	public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
		BeanUtils.copyProperties(source, target, ignoreProperties);
	}

	/**
	 * 属性拷贝并限定可编辑类型（委托 {@link BeanUtils#copyProperties(Object, Object, Class)}）。
	 */
	public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
		BeanUtils.copyProperties(source, target, editable);
	}
}
