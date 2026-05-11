package org.peach.common.mvc.jackson.sensitive;

import java.util.regex.Pattern;

import org.peach.common.mvc.annotation.json.Sensitive;
import org.peach.common.mvc.annotation.json.SensitiveType;
import org.springframework.util.StringUtils;

/**
 * 字符串脱敏规则实现（供 Jackson 序列化器与其它展示层复用）。
 *
 * @author leiyangjun
 */
public final class SensitiveMaskUtils {

	private static final Pattern NON_DIGITS = Pattern.compile("\\D");

	private static final Pattern WHITESPACE = Pattern.compile("\\s");

	/** 纯数字串（手机号等） */
	private static final Pattern ALL_DIGITS = Pattern.compile("^\\d+$");

	/** 18 位二代证：前 17 位数字 + 末位数字或 X */
	private static final Pattern ID_18 = Pattern.compile("(?i)^\\d{17}[0-9x]$");

	/** 15 位一代证：全数字 */
	private static final Pattern ID_15_ALL_DIGITS = Pattern.compile("^\\d{15}$");

	private static final int MOBILE_LEN = 11;

	private SensitiveMaskUtils() {
	}

	/**
	 * 按注解策略脱敏；空白串原样返回。
	 */
	public static String mask(String raw, Sensitive rule) {
		if (!StringUtils.hasText(raw)) {
			return raw == null ? null : raw;
		}
		String value = raw.trim();
		return mask(value, rule.value(), rule.prefixKeep(), rule.suffixKeep());
	}

	/**
	 * 按类型脱敏（非 CUSTOM 时忽略前后保留长度）。
	 */
	public static String mask(String value, SensitiveType type, int prefixKeep, int suffixKeep) {
		if (!StringUtils.hasText(value)) {
			return value;
		}
		return switch (type) {
			case MOBILE -> maskMobile(value);
			case ID_CARD -> maskIdCard(value);
			case EMAIL -> maskEmail(value);
			case CHINESE_NAME -> maskChineseName(value);
			case BANK_CARD -> maskBankCard(value);
			case FIXED_PHONE -> maskFixedPhone(value);
			case CUSTOM -> maskCustom(value, prefixKeep, suffixKeep);
		};
	}

	static String maskMobile(String value) {
		String digits = NON_DIGITS.matcher(value).replaceAll("");
		if (digits.length() == MOBILE_LEN && ALL_DIGITS.matcher(digits).matches()) {
			return digits.substring(0, 3) + "****" + digits.substring(7);
		}
		return maskCustom(value, 3, 4);
	}

	static String maskIdCard(String value) {
		String compact = WHITESPACE.matcher(value).replaceAll("");
		if (compact.length() == 18 && ID_18.matcher(compact).matches()) {
			return compact.substring(0, 6) + "********" + compact.substring(14);
		}
		if (compact.length() == 15 && ID_15_ALL_DIGITS.matcher(compact).matches()) {
			return compact.substring(0, 6) + "******" + compact.substring(compact.length() - 4);
		}
		return maskCustom(compact, 4, 4);
	}

	static String maskEmail(String value) {
		int at = value.indexOf('@');
		if (at <= 0 || at == value.length() - 1) {
			return maskCustom(value, 1, 1);
		}
		String local = value.substring(0, at);
		String domain = value.substring(at);
		if (local.length() <= 1) {
			return "*" + domain;
		}
		if (local.length() == 2) {
			return local.charAt(0) + "*" + domain;
		}
		return local.charAt(0) + "***" + local.substring(local.length() - 1) + domain;
	}

	static String maskChineseName(String value) {
		int len = value.codePointCount(0, value.length());
		if (len <= 1) {
			return "*";
		}
		if (len == 2) {
			return firstChar(value) + "*";
		}
		return firstChar(value) + repeatStar(len - 2) + lastChar(value);
	}

	private static String firstChar(String s) {
		int cp = s.codePointAt(0);
		return new String(Character.toChars(cp));
	}

	private static String lastChar(String s) {
		int i = s.offsetByCodePoints(0, s.codePointCount(0, s.length()) - 1);
		int cp = s.codePointAt(i);
		return new String(Character.toChars(cp));
	}

	private static String repeatStar(int n) {
		return "*".repeat(Math.max(0, n));
	}

	static String maskBankCard(String value) {
		String digits = NON_DIGITS.matcher(value).replaceAll("");
		if (digits.length() < 8) {
			return maskCustom(digits, 0, 4);
		}
		String last4 = digits.substring(digits.length() - 4);
		return "**** **** **** " + last4;
	}

	static String maskFixedPhone(String value) {
		String digits = NON_DIGITS.matcher(value).replaceAll("");
		if (digits.length() >= 7) {
			int showHead = Math.min(3, digits.length() - 4);
			return digits.substring(0, showHead) + "****" + digits.substring(digits.length() - 4);
		}
		return maskCustom(value, 2, 2);
	}

	static String maskCustom(String value, int prefixKeep, int suffixKeep) {
		int pre = Math.max(0, prefixKeep);
		int suf = Math.max(0, suffixKeep);
		int len = value.length();
		if (len == 0) {
			return value;
		}
		if (pre + suf >= len) {
			return "*".repeat(len);
		}
		return value.substring(0, pre) + "****" + value.substring(len - suf);
	}
}
