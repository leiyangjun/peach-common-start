package org.peach.common.mvc.jackson.sensitive;

import org.peach.common.mvc.annotation.json.Sensitive;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 将标记为 {@link Sensitive} 的字符串属性写出为脱敏后的文本。
 *
 * @author leiyangjun
 */
public class SensitiveJsonSerializer extends ValueSerializer<Object> {

	private final Sensitive rule;

	public SensitiveJsonSerializer(Sensitive rule) {
		this.rule = rule;
	}

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializationContext serializers) {
		if (value == null) {
			gen.writeNull();
			return;
		}
		gen.writeString(SensitiveMaskUtils.mask(value.toString(), rule));
	}
}
