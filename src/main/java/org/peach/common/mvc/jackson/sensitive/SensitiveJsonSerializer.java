package org.peach.common.mvc.jackson.sensitive;

import java.io.IOException;

import org.peach.common.mvc.annotation.json.Sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * 将标记为 {@link Sensitive} 的字符串属性写出为脱敏后的文本。
 *
 * @author leiyangjun
 */
public class SensitiveJsonSerializer extends JsonSerializer<Object> {

	private final Sensitive rule;

	public SensitiveJsonSerializer(Sensitive rule) {
		this.rule = rule;
	}

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}
		gen.writeString(SensitiveMaskUtils.mask(value.toString(), rule));
	}
}
