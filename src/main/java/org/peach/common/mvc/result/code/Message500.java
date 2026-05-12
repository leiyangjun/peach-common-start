package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 500 系统异常态下的消息码；对外 {@code msg} 宜固定为 {@link #INTERNAL}，异常详情仅写服务端日志。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum Message500 implements MessageCode {

	/** 5001 系统内部错误（对外固定文案，不透传异常详情） */
	INTERNAL(5001, "系统内部错误");

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
