package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 200 成功态下的提示编码与默认文案（当前仅 {@link #OK}）。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum ApiResultHttp200 implements ApiResultCodeSpec {

	/** 2001 操作成功 */
	OK(2001, "操作成功");

	private final int hintCode;
	private final String defaultMessage;

	@Override
	public int family() {
		return 200;
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

