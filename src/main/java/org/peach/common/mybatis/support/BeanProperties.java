package org.peach.common.mybatis.support;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * 实体属性读取（反射），避免 {@code JSON.toJSONString} + {@code JSONObject} 的序列化开销与类型歧义。
 *
 * @author leiyangjun
 */
public final class BeanProperties {

	private BeanProperties() {
	}

	/**
	 * 读取 JavaBean 属性；{@code bean} 为 {@link Class} 或 {@code null} 时返回 {@code null}。
	 */
	public static Object read(Object bean, String name) {
		if (bean == null || bean instanceof Class<?>) {
			return null;
		}
		try {
			return FieldUtils.readField(bean, name, true);
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	/**
	 * 是否视为「空」主键/条件：{@code null} 或空字符串（仅对 {@link CharSequence}）。
	 */
	public static boolean isEmptyKey(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof CharSequence) {
			return ((CharSequence) value).length() == 0;
		}
		return false;
	}
}

