package org.peach.common.mvc.jackson.sensitive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.peach.common.mvc.annotation.json.SensitiveType;

/**
 * {@link SensitiveMaskUtils} 规则单测。
 */
class SensitiveMaskUtilsTest {

	@Test
	void maskMobile_standard11() {
		assertEquals("138****5678", SensitiveMaskUtils.mask("13812345678", SensitiveType.MOBILE, 0, 0));
	}

	@Test
	void maskIdCard_18() {
		assertEquals("110101********5236",
				SensitiveMaskUtils.mask("110101199003075236", SensitiveType.ID_CARD, 0, 0));
	}

	@Test
	void maskIdCard_18_tail_X() {
		assertEquals("110101********523x",
				SensitiveMaskUtils.mask("11010119900307523x", SensitiveType.ID_CARD, 0, 0));
	}

	@Test
	void maskEmail() {
		assertEquals("a***z@test.com", SensitiveMaskUtils.mask("abcxyz@test.com", SensitiveType.EMAIL, 0, 0));
	}

	@Test
	void maskChineseName_threeChars() {
		assertEquals("刘*菲", SensitiveMaskUtils.mask("刘亦菲", SensitiveType.CHINESE_NAME, 0, 0));
	}

	@Test
	void maskBankCard() {
		String out = SensitiveMaskUtils.mask("6222021234567890123", SensitiveType.BANK_CARD, 0, 0);
		assertEquals("**** **** **** 0123", out);
	}

	@Test
	void maskCustom() {
		assertEquals("12****89", SensitiveMaskUtils.mask("123456789", SensitiveType.CUSTOM, 2, 2));
	}
}
