package org.peach.common.mvc.jackson.sensitive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.peach.common.mvc.annotation.json.Sensitive;
import org.peach.common.mvc.annotation.json.SensitiveType;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;

/**
 * Jackson 模块写出 JSON 时脱敏。
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

	/** 注解仅在私有字段、序列化走 getter（与 Lombok {@code @Data} 常见形态一致）。 */
	static class PrivateFieldAnnVo {

		@Sensitive(SensitiveType.MOBILE)
		private String phone = "13812345678";

		public String getPhone() {
			return phone;
		}
	}

	@Test
	void json_contains_masked_phone() throws JacksonException {
		ObjectMapper mapper = JsonMapper.builder().addModule(new SensitiveJacksonModule()).build();
		String json = mapper.writeValueAsString(new SampleVo());
		assertTrue(json.contains("138****5678"), json);
		assertTrue(json.contains("visible"));
	}

	@Test
	void json_masks_field_only_sensitive_on_private_property() throws JacksonException {
		ObjectMapper mapper = JsonMapper.builder().addModule(new SensitiveJacksonModule()).build();
		String json = mapper.writeValueAsString(new PrivateFieldAnnVo());
		assertTrue(json.contains("138****5678"), json);
	}
}
