package org.peach.common.mvc.jackson.sensitive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.peach.common.mvc.annotation.json.Sensitive;
import org.peach.common.mvc.annotation.json.SensitiveType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson 模块写出 JSON 时脱敏。
 *
 * @author leiyangjun
 */
class SensitiveJacksonModuleTest {

	static class SampleVo {

		public String plain = "visible";

		@Sensitive(SensitiveType.MOBILE)
		public String phone = "13812345678";

		public String getPlain() {
			return plain;
		}

		public String getPhone() {
			return phone;
		}
	}

	@Test
	void json_contains_masked_phone() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new SensitiveJacksonModule());
		String json = mapper.writeValueAsString(new SampleVo());
		assertTrue(json.contains("138****5678"), json);
		assertTrue(json.contains("visible"));
	}
}
