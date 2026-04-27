package org.peach.common.mvc.result.code;

/**
 * 多模型程序化校验失败时使用的业务码（提示段末两位 &gt; 20，与框架保留号段区分）。
 * <p>
 * 具体违背原因见响应 {@code msg}，由 {@link org.peach.common.mvc.validation.ValidationUtils#valid(Object...)} 聚合。
 * </p>
 *
 * @author leiyangjun
 */
public enum ModelValidateBizCode implements ApiResultCustomCode {

	/** 4021：参数校验未通过（明细在 msg） */
	AGGREGATE(4021, "参数校验未通过");

	private final int code;
	private final String msg;

	ModelValidateBizCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	@Override
	public int code() {
		return code;
	}

	@Override
	public String msg() {
		return msg;
	}
}

