package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * 框架层 HTTP 400 提示码（号段 {@code 4020–4099} 内子集），仅供启动器内部装配非成功响应体或抛
 * {@link org.peach.common.mvc.exception.BizException}；部分 {@link #msg()} 含单个 {@code %s} 供首条校验说明格式化。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum Message400 implements MessageCode {

	/** 首条字段/请求体校验说明；{@code msg} 含 {@code %s} */
	METHOD_ARG_NOT_VALID(4020, "参数校验错误：%s"),

	/** 首条绑定错误说明 */
	BIND_FAILURE(4021, "参数绑定失败：%s"),

	/** 首条约束违背说明 */
	CONSTRAINT_VIOLATION(4022, "参数约束不满足：%s"),

	BODY_NOT_READABLE(4023, "请求体格式不正确或无法解析"),

	MISSING_PARAMETER(4024, "缺少必要请求参数"),

	MISSING_API_VERSION(4025, "缺少 API 版本信息"),

	INVALID_API_VERSION(4026, "API 版本无效或不受支持"),

	/** 程序化校验首条说明 */
	FIRST_VALIDATE_FAILURE(4027, "参数校验未通过：%s");

	private final int code;
	private final String msg;

	@Override
	public int code() {
		return code;
	}

	@Override
	public String msg() {
		return msg;
	}

	@Override
	public boolean frameworkBuiltinMessageCode() {
		return true;
	}
}
