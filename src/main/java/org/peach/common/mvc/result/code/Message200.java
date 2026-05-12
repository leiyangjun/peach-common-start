package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 200 成功态下的消息码与默认文案（当前仅 {@link #OK}）。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum Message200 implements MessageCode {

	/** 2001 操作成功 */
	OK(2001, "操作成功");

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
}
