package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 401 未授权态下的提示编码与默认文案。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum ApiResultHttp401 implements ApiResultCodeSpec {

	/** 4001 用户名或密码错误 */
	USERNAME_OR_PASSWORD(4001, "用户名或密码错误"),

	/** 4002 未登录或令牌无效 */
	TOKEN_INVALID(4002, "未登录或令牌无效");

	private final int hintCode;
	private final String defaultMessage;

	@Override
	public int family() {
		return 401;
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

