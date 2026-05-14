package org.peach.common.mvc.exception;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.autoconfigure.ModuleCodeCache;
import org.peach.common.mvc.result.code.MessageCode;
import org.springframework.http.HttpStatus;

/**
 * 框架内部非 200 响应体：与 {@link BizException}、{@link GlobalExceptionHandler} 同包，类为包可见。
 * <p>
 * 业务码前缀经 {@link ModuleCodeCache#getModule()} 获取，与
 * {@link org.peach.common.mvc.autoconfigure.ModuleCodeCheckConfiguration}、
 * {@link org.peach.common.mvc.autoconfigure.SpringApplicationModuleProperties}
 * 约定一致。
 * </p>
 * <p>
 * HTTP 响应体由 {@code ResponseEntity<ErrorResult>} 约定，经 Spring MVC / Jackson 对
 * {@link #getCode()}、 {@link #getMsg()} 等完成 JSON 序列化，与 {@link #toString()} 无关。
 * </p>
 */
final class ErrorResult {

	private final String code;
	private String msg;

	private ErrorResult(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	/**
	 * 仅用于日志等场景的简要文本输出；与 {@code ResponseEntity<ErrorResult>} 的 JSON 响应体无关（响应由
	 * Jackson 序列化属性）。
	 */
	@Override
	public String toString() {
		return "ErrorResult[code=" + code + ", msg=" + msg + "]";
	}

	static ErrorResult validWarn(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.getModule() + HttpStatus.BAD_REQUEST.value() + messageCode.code(),
				messageCode.msg());
	}

	static ErrorResult validWarn(MessageCode messageCode, String validMsg) {
		String template = messageCode.msg();
		String msg;
		if (template != null && template.contains("%s")) {
			String slot = StringUtils.isNotBlank(validMsg) ? validMsg : "—";
			msg = String.format(template, slot);
		} else {
			msg = Objects.toString(template, "") + Objects.toString(validMsg, "");
		}
		return new ErrorResult(ModuleCodeCache.getModule() + HttpStatus.BAD_REQUEST.value() + messageCode.code(), msg);
	}

	static ErrorResult error(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.getModule() + HttpStatus.INTERNAL_SERVER_ERROR.value() + messageCode.code(),
				messageCode.msg());
	}

	static ErrorResult unauthorized(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.getModule() + HttpStatus.UNAUTHORIZED.value() + messageCode.code(),
				messageCode.msg());
	}

	static ErrorResult forbidden(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.getModule() + HttpStatus.FORBIDDEN.value() + messageCode.code(),
				messageCode.msg());
	}

	static ErrorResult notFound(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.getModule() + HttpStatus.NOT_FOUND.value() + messageCode.code(),
				messageCode.msg());
	}
}
