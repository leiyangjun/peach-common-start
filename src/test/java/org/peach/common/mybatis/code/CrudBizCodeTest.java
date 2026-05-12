package org.peach.common.mybatis.code;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.peach.common.mvc.result.code.ApiResultCodeRules;

/**
 * {@link CrudBizCode} 与框架保留 HTTP 400 号段（4000–4099）一致性校验。
 */
class CrudBizCodeTest {

	private static final int FRAMEWORK_RESERVED_400_MIN = 4000;
	private static final int FRAMEWORK_RESERVED_400_MAX = 4099;

	@Test
	void allCodesInFrameworkReserved400Range() {
		for (CrudBizCode c : CrudBizCode.values()) {
			int h = ApiResultCodeRules.normalizeMsgSegment(c.code());
			assertTrue(h >= FRAMEWORK_RESERVED_400_MIN && h <= FRAMEWORK_RESERVED_400_MAX,
					() -> "CrudBizCode 须落在框架保留段 " + FRAMEWORK_RESERVED_400_MIN + "–" + FRAMEWORK_RESERVED_400_MAX + ": " + c.name());
		}
	}
}
