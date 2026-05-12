package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 403 禁止访问态下的消息码与默认文案。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum Message403 implements MessageCode {

	/** 4003 没有授权访问 */
	NO_ACCESS(4003, "没有授权访问！");

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
