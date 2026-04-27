package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 403 禁止访问态下的提示编码与默认文案。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum ApiResultHttp403 implements ApiResultCodeSpec {

	/** 4002 没有访问权限 */
	NO_ACCESS(4002, "没有访问权限");

	private final int hintCode;
	private final String defaultMessage;

	@Override
	public int family() {
		return 403;
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

