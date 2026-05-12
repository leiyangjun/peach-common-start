package org.peach.common.mvc.result.code;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 404 资源不存在态下的消息码（仅框架全局异常处理等使用，业务服务不得声明 404 族业务码）。
 *
 * @author leiyangjun
 */
@RequiredArgsConstructor
public enum Message404 implements MessageCode {

	/** 4004 没有该资源可供访问 */
	NOT_FOUND(4004, "没有该资源可供访问！");

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
