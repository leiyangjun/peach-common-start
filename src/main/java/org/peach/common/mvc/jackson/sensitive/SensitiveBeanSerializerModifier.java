package org.peach.common.mvc.jackson.sensitive;

import java.lang.reflect.Field;
import java.util.List;

import org.peach.common.mvc.annotation.json.Sensitive;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/**
 * 为带 {@link Sensitive} 且类型为 {@link String} 的 Bean 属性挂载 {@link SensitiveJsonSerializer}。
 *
 * @author leiyangjun
 */
public class SensitiveBeanSerializerModifier extends BeanSerializerModifier {
	
	private static final long serialVersionUID = 1L;

	@Override
	public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
			List<BeanPropertyWriter> beanProperties) {
		for (BeanPropertyWriter writer : beanProperties) {
			Sensitive ann = resolveSensitive(writer, beanDesc);
			if (ann == null) {
				continue;
			}
			if (!writer.getType().isTypeOrSubTypeOf(String.class)) {
				continue;
			}
			writer.assignSerializer(new SensitiveJsonSerializer(ann));
		}
		return beanProperties;
	}

	/**
	 * 优先取序列化成员上的注解；若为空则从同名字段解析（Lombok {@code @Data} 等常把 {@link Sensitive} 标在字段上、写出走 getter）。
	 */
	static Sensitive resolveSensitive(BeanPropertyWriter writer, BeanDescription beanDesc) {
		Sensitive ann = writer.getAnnotation(Sensitive.class);
		if (ann != null) {
			return ann;
		}
		String prop = writer.getName();
		if (prop == null) {
			return null;
		}
		for (Class<?> c = beanDesc.getBeanClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(prop);
				return f.getAnnotation(Sensitive.class);
			} catch (@SuppressWarnings("unused") NoSuchFieldException ignored) {
				// 继续父类
			}
		}
		return null;
	}
}
