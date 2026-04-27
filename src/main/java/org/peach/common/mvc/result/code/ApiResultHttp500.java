package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 500 系统异常态下的提示编码；实际返回宜使用异常信息。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum ApiResultHttp500 implements ApiResultCodeSpec {

	/** 5001 系统内部错误（获取抛出异常信息） */
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

