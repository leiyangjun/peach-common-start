package org.peach.common.mvc.exception;

import java.io.Serial;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mvc.result.code.ApiResultCodeRules;
import org.peach.common.mvc.result.code.ApiResultCustomCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 业务异常（非受检）：携带 {@link ApiResultCustomCode}，由 {@link GlobalExceptionHandler} 转为 {@link ApiResult}
 * 并统一打日志（客户端错误多为 WARN，服务端错误多为 ERROR，含 cause 时打印完整堆栈）。
 * <p>
 * 业界常见写法要点：{@code serialVersionUID}、明确 HTTP 语义、可链式原因 {@link #getCause()}、工厂方法表达意图。<br>
 * 不在构造方法内打日志，避免副作用与重复记录；日志仅在全局处理器中输出。
 * </p>
 * <p>
 * 若 {@link #responseMessage} 非空，全局处理时优先将其作为 {@link ApiResult#getMsg()}（如聚合校验明细），
 * 否则使用 {@link ApiResultCustomCode#msg()}。
 * </p>
 * <p>
 * 当前与 {@link ApiResult#fail400(org.peach.common.mvc.result.code.ApiResultCustomCode)} /
 * {@link ApiResult#fail500(org.peach.common.mvc.result.code.ApiResultCustomCode)} 对齐，仅支持 HTTP 400 / 500。
 * </p>
 *
 * @author leiyangjun
 */
@Getter
public class BizException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 与响应 HTTP 状态一致，用于网关、监控与客户端处理。 */
	private final HttpStatus httpStatus;

	/** 业务错误码与默认文案，将参与统一 {@code code} / {@code msg} 拼接。 */
	private final ApiResultCustomCode customCode;

	/**
	 * 非空时作为 {@link ApiResult#getMsg()}，与 {@link #customCode}{@code .msg()} 解耦（如字段级校验汇总）。
	 */
	private final String responseMessage;

	public BizException(HttpStatus httpStatus, ApiResultCustomCode customCode) {
		this(httpStatus, customCode, null, null);
	}

	public BizException(HttpStatus httpStatus, ApiResultCustomCode customCode, Throwable cause) {
		this(httpStatus, customCode, null, cause);
	}

	private BizException(HttpStatus httpStatus, ApiResultCustomCode customCode, String responseMessage, Throwable cause) {
		super(StringUtils.isNotBlank(responseMessage) ? responseMessage : resolveMessage(customCode), cause);
		this.httpStatus = validateStatus(httpStatus);
		this.customCode = validateCode(customCode);
		this.responseMessage = StringUtils.isBlank(responseMessage) ? null : responseMessage;
	}

	private static String resolveMessage(ApiResultCustomCode customCode) {
		return Objects.requireNonNull(customCode, "customCode").msg();
	}

	private static HttpStatus validateStatus(HttpStatus httpStatus) {
		if (httpStatus == null) {
			throw new IllegalArgumentException("httpStatus 不能为空");
		}
		if (httpStatus != HttpStatus.BAD_REQUEST && httpStatus != HttpStatus.INTERNAL_SERVER_ERROR) {
			throw new IllegalArgumentException(
					"BizException 当前仅支持 HttpStatus.BAD_REQUEST(400) 或 INTERNAL_SERVER_ERROR(500)");
		}
		return httpStatus;
	}

	private static ApiResultCustomCode validateCode(ApiResultCustomCode customCode) {
		ApiResultCustomCode c = Objects.requireNonNull(customCode, "customCode");
		ApiResultCodeRules.assertCustomBizHintTail(c.code());
		if (c.msg() == null) {
			throw new IllegalArgumentException("customCode.msg 不能为 null");
		}
		return c;
	}

	/** 客户端可修正类业务错误（映射 400 + {@link ApiResult#fail400(ApiResultCustomCode)}）。 */
	public static BizException badRequest(ApiResultCustomCode code) {
		return new BizException(HttpStatus.BAD_REQUEST, code);
	}

	/**
	 * 400：使用 {@code responseMessage} 作为对外 {@code msg}（如校验明细），业务码仍取自 {@code code}。
	 */
	public static BizException badRequest(ApiResultCustomCode code, String responseMessage) {
		return new BizException(HttpStatus.BAD_REQUEST, code, responseMessage, null);
	}

	/** 同上，保留原始异常链便于排查。 */
	public static BizException badRequest(ApiResultCustomCode code, Throwable cause) {
		return new BizException(HttpStatus.BAD_REQUEST, code, cause);
	}

	/** 服务端或不可预期资源类业务失败（映射 500 + {@link ApiResult#fail500(ApiResultCustomCode)}）。 */
	public static BizException serverError(ApiResultCustomCode code) {
		return new BizException(HttpStatus.INTERNAL_SERVER_ERROR, code);
	}

	/** 同上，保留原始异常链。 */
	public static BizException serverError(ApiResultCustomCode code, Throwable cause) {
		return new BizException(HttpStatus.INTERNAL_SERVER_ERROR, code, cause);
	}
}

