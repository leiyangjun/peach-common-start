package org.peach.common.mybatis.code;

import org.junit.jupiter.api.Test;
import org.peach.common.mvc.result.code.ApiResultCodeRules;

/**
 * {@link CrudBizCode} 与框架编码规则一致性校验。
 */
class CrudBizCodeTest {

	@Test
	void allCodesSatisfyCustomTailRule() {
		for (CrudBizCode c : CrudBizCode.values()) {
			ApiResultCodeRules.assertCustomBizHintTail(c.code());
		}
	}
}
