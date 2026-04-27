package org.peach.common.mvc.result.code;

/**
 * {@link ApiResultCustomCode} 与业务异常共用的编码规则校验。
 *
 * @author leiyangjun
 */
public final class ApiResultCodeRules {

	private ApiResultCodeRules() {
	}

	/**
	 * 自定义业务提示编码末两位须严格大于 20（≤20 为框架保留号段）。
	 *
	 * @param hintCode 四位提示段数值
	 * @throws IllegalArgumentException 不满足规则时
	 */
	public static void assertCustomBizHintTail(int hintCode) {
		int tail = Math.abs(hintCode) % 100;
		if (tail <= 20) {
			throw new IllegalArgumentException(
					"自定义错误提示编码末两位须大于20（当前末两位=" + tail + "，完整提示段=" + hintCode + "）");
		}
	}
}

