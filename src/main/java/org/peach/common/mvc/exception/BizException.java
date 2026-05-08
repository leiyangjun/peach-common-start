package org.peach.common.mvc.exception;

import java.io.Serial;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mvc.result.code.ApiResultCodeRules;
import org.peach.common.mvc.result.code.ApiResultCodeSpec;
import org.peach.common.mvc.result.code.ApiResultCustomCode;
import org.peach.common.mvc.result.code.ApiResultHttp401;
import org.peach.common.mvc.result.code.ApiResultHttp403;
import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 业务异常（非受检）：由 {@link GlobalExceptionHandler} 转为 {@link ApiResult} 并统一打日志。
 * <p>
 * 请使用静态工厂方法构造，勿直接 {@code new}（构造器均私有）。
 * </p>
 * <p>
 * 与 {@link ApiResult#fail400} / {@link ApiResult#fail401} / {@link ApiResult#fail403} /
 * {@link ApiResult#fail500} 语义对齐。
 * </p>
 *
 * @author leiyangjun
 */
@Getter
public class BizException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final HttpStatus httpStatus;

	/** HTTP 400 / 500 时使用；与 {@link #httpCodeSpec} 互斥。 */
	private final ApiResultCustomCode customCode;

	/** HTTP 401 / 403 时使用；与 {@link #customCode} 互斥。 */
	private final ApiResultCodeSpec httpCodeSpec;

	/**
	 * 非空时作为对外 {@link ApiResult#getMsg()}，否则使用枚举 / 自定义码默认文案。
	 */
	private final String responseMessage;

	// --- 400 / 500：ApiResultCustomCode ---

	private BizException(HttpStatus httpStatus, ApiResultCustomCode customCode, String responseMessage, Throwable cause) {
		super(resolveMessageForCustom(customCode, responseMessage), cause);
		this.httpStatus = validateCustomHttpStatus(httpStatus);
		this.customCode = validateCode(customCode);
		this.httpCodeSpec = null;
		this.responseMessage = blankToNull(responseMessage);
	}

	private static HttpStatus validateCustomHttpStatus(HttpStatus httpStatus) {
		if (httpStatus == null) {
			throw new IllegalArgumentException("httpStatus 不能为空");
		}
		if (httpStatus != HttpStatus.BAD_REQUEST && httpStatus != HttpStatus.INTERNAL_SERVER_ERROR) {
			throw new IllegalArgumentException(
					"BizException（customCode）仅支持 BAD_REQUEST(400) 或 INTERNAL_SERVER_ERROR(500)");
		}
		return httpStatus;
	}

	private static String resolveMessageForCustom(ApiResultCustomCode customCode, String responseMessage) {
		if (StringUtils.isNotBlank(responseMessage)) {
			return responseMessage;
		}
		return Objects.requireNonNull(customCode, "customCode").msg();
	}

	// --- 401 / 403：ApiResultCodeSpec ---

	private BizException(HttpStatus httpStatus, ApiResultCodeSpec httpCodeSpec, String responseMessage, Throwable cause) {
		super(resolveMessageForSpec(httpCodeSpec, responseMessage), cause);
		this.httpStatus = validateHttpSpecStatus(httpStatus, httpCodeSpec);
		this.httpCodeSpec = Objects.requireNonNull(httpCodeSpec, "httpCodeSpec");
		this.customCode = null;
		this.responseMessage = blankToNull(responseMessage);
	}

	private static String resolveMessageForSpec(ApiResultCodeSpec spec, String responseMessage) {
		if (StringUtils.isNotBlank(responseMessage)) {
			return responseMessage;
		}
		return Objects.requireNonNull(spec, "httpCodeSpec").defaultMessage();
	}

	private static HttpStatus validateHttpSpecStatus(HttpStatus httpStatus, ApiResultCodeSpec spec) {
		if (httpStatus == null) {
			throw new IllegalArgumentException("httpStatus 不能为空");
		}
		int fam = spec.family();
		if (httpStatus == HttpStatus.UNAUTHORIZED && fam == 401) {
			return httpStatus;
		}
		if (httpStatus == HttpStatus.FORBIDDEN && fam == 403) {
			return httpStatus;
		}
		throw new IllegalArgumentException(
				"BizException（httpCodeSpec）须为 UNAUTHORIZED+401 族 或 FORBIDDEN+403 族，当前 httpStatus=" + httpStatus
						+ ", spec.family=" + fam);
	}

	private static String blankToNull(String s) {
		return StringUtils.isBlank(s) ? null : s;
	}

	private static ApiResultCustomCode validateCode(ApiResultCustomCode customCode) {
		ApiResultCustomCode c = Objects.requireNonNull(customCode, "customCode");
		ApiResultCodeRules.assertCustomBizHintTail(c.code());
		if (c.msg() == null) {
			throw new IllegalArgumentException("customCode.msg 不能为 null");
		}
		return c;
	}

	// ---------- 400 ----------

	/** 客户端可修正类业务错误（400 + {@link ApiResult#fail400(ApiResultCustomCode)}）。 */
	public static BizException badRequest(ApiResultCustomCode code) {
		return new BizException(HttpStatus.BAD_REQUEST, code, null, null);
	}

	/**
	 * 400：使用 {@code responseMessage} 作为对外 {@code msg}。
	 */
	public static BizException badRequest(ApiResultCustomCode code, String responseMessage) {
		return new BizException(HttpStatus.BAD_REQUEST, code, responseMessage, null);
	}

	/** 400：保留原始异常链。 */
	public static BizException badRequest(ApiResultCustomCode code, Throwable cause) {
		return new BizException(HttpStatus.BAD_REQUEST, code, null, cause);
	}

	// ---------- 401 ----------

	/**
	 * 401：默认使用 {@link ApiResultHttp401#TOKEN_INVALID}（未登录或令牌无效）。
	 */
	public static BizException unauthorized() {
		return unauthorized(ApiResultHttp401.TOKEN_INVALID);
	}

	/** 未授权（401 + {@link ApiResult#fail401(ApiResultHttp401)}）。 */
	public static BizException unauthorized(ApiResultHttp401 spec) {
		return new BizException(HttpStatus.UNAUTHORIZED, spec, null, null);
	}

	/** 401：覆盖对外 {@code msg}。 */
	public static BizException unauthorized(ApiResultHttp401 spec, String responseMessage) {
		return new BizException(HttpStatus.UNAUTHORIZED, spec, responseMessage, null);
	}

	/** 401：保留原始异常链。 */
	public static BizException unauthorized(ApiResultHttp401 spec, Throwable cause) {
		return new BizException(HttpStatus.UNAUTHORIZED, spec, null, cause);
	}

	// ---------- 403 ----------

	/**
	 * 403：默认使用 {@link ApiResultHttp403#NO_ACCESS}（没有访问权限）。
	 */
	public static BizException forbidden() {
		return forbidden(ApiResultHttp403.NO_ACCESS);
	}

	/** 禁止访问（403 + {@link ApiResult#fail403(ApiResultHttp403)}）。 */
	public static BizException forbidden(ApiResultHttp403 spec) {
		return new BizException(HttpStatus.FORBIDDEN, spec, null, null);
	}

	/** 403：覆盖对外 {@code msg}。 */
	public static BizException forbidden(ApiResultHttp403 spec, String responseMessage) {
		return new BizException(HttpStatus.FORBIDDEN, spec, responseMessage, null);
	}

	/** 403：保留原始异常链。 */
	public static BizException forbidden(ApiResultHttp403 spec, Throwable cause) {
		return new BizException(HttpStatus.FORBIDDEN, spec, null, cause);
	}

	// ---------- 500 ----------

	/** 服务端类业务失败（500 + {@link ApiResult#fail500(ApiResultCustomCode)}）。 */
	public static BizException serverError(ApiResultCustomCode code) {
		return new BizException(HttpStatus.INTERNAL_SERVER_ERROR, code, null, null);
	}

	/** 500：保留原始异常链。 */
	public static BizException serverError(ApiResultCustomCode code, Throwable cause) {
		return new BizException(HttpStatus.INTERNAL_SERVER_ERROR, code, null, cause);
	}
}
