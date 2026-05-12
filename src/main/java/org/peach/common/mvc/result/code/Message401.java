package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 401 未授权态下的消息码与默认文案。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum Message401 implements MessageCode {

	/** 4001 用户名或密码错误 */
	USERNAME_OR_PASSWORD(4001, "用户名或密码错误"),

	/** 4002 未登录或令牌无效 */
	TOKEN_INVALID(4002, "未登录或令牌无效");

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
