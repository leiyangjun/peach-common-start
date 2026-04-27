package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 400 请求错误态下的提示编码；默认文案为「参数非法」，实际返回宜使用校验框架给出的信息。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum ApiResultHttp400 implements ApiResultCodeSpec {

	/** 4003 参数非法（由校验框架返回信息） */
	INVALID_PARAM(4003, "参数非法"),

	/** 4004 参数校验错误（统一错误信息） */
	UNIFIED_VALIDATE_ERROR(4004, "参数校验错误");

	private final int hintCode;
	private final String defaultMessage;

	@Override
	public int family() {
		return 400;
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

