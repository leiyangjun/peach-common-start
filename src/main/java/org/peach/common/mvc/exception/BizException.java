package org.peach.common.mvc.exception;

import java.io.Serial;
import java.util.Objects;

import org.peach.common.mvc.result.code.MessageCode;
import org.springframework.http.HttpStatus;

/**
 * 业务异常：{@link Throwable#getMessage()} 与 {@link ErrorResult#toString()} 相同，便于日志与排查；
 * 与 {@link GlobalExceptionHandler} 返回的 {@link org.springframework.http.ResponseEntity}{@code <ErrorResult>} 无必然格式关联（响应体由 Jackson 序列化）。
 *
 * @author leiyangjun
 */
public class BizException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final HttpStatus httpStatus;
	private final ErrorResult errorResult;

	private BizException(HttpStatus httpStatus, ErrorResult errorResult, Throwable cause) {
		super(errorResult.toString(), cause);
		this.httpStatus = Objects.requireNonNull(httpStatus, "httpStatus");
		this.errorResult = Objects.requireNonNull(errorResult, "errorResult");
	}

	private BizException(HttpStatus httpStatus, ErrorResult errorResult) {
		super(errorResult.toString(), null);
		this.httpStatus = Objects.requireNonNull(httpStatus, "httpStatus");
		this.errorResult = Objects.requireNonNull(errorResult, "errorResult");
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public ErrorResult getErrorResult() {
		return errorResult;
	}

	/**
	 * @Title: validWarn
	 * @Description: 抛出校验信息警告，不打印堆栈信息，一般用于系统统一校验参数校验，前端返回400状态码
	 */
	public static BizException validWarn(MessageCode code) {
		return new BizException(HttpStatus.BAD_REQUEST, ErrorResult.validWarn(code));
	}

	/**
	 * @Title: validWarn
	 * @Description: 抛出校验信息警告，不打印堆栈信息，一般用于统一校验处理，前端返回400状态码
	 */
	public static BizException validWarn(MessageCode code, String validMsg) {
		ErrorResult er = ErrorResult.validWarn(code, validMsg);
		return new BizException(HttpStatus.BAD_REQUEST, er);
	}

	/**
	 * @Title: error
	 * @Description: 抛出错误异常信息,不带打印堆栈信息，前端返回500错误状态码
	 */
	public static BizException error(MessageCode code) {
		return new BizException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResult.error(code));
	}

	/**
	 * @Title: error
	 * @Description: 抛出错误异常信息,带打印堆栈信息，前端返回500错误状态码
	 */
	public static BizException error(MessageCode code, Throwable cause) {
		return new BizException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResult.error(code), cause);
	}
}
