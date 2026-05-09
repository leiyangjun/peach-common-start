package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 500 系统异常态下的提示编码；对外 {@code msg} 宜固定为 {@link #INTERNAL}，异常详情仅写服务端日志。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum ApiResultHttp500 implements ApiResultCodeSpec {

	/** 5001 系统内部错误（对外固定文案，不透传异常详情） */
	INTERNAL(5001, "系统内部错误");

	private final int hintCode;
	private final String defaultMessage;

	@Override
	public int family() {
		return 500;
	}

	@Override
	public int hintCode() {
		return hintCode;
	}

	@Override
	public String defaultMessage() {
		return defaultMessage;
	}
}

