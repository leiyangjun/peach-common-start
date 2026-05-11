package org.peach.common.mvc.jackson.sensitive;

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
			Sensitive ann = writer.getAnnotation(Sensitive.class);
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
}
