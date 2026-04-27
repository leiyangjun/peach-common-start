package org.peach.common.mvc.exception;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mybatis.exception.PersistenceException;
import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mvc.result.code.ApiResultCustomCode;
import org.peach.common.mvc.result.code.ApiResultHttp400;
import org.peach.common.mvc.result.code.ApiResultHttp401;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理：将异常转为 {@link ApiResult}，HTTP 状态与业务语义对齐，并统一记录日志。
 * <p>
 * <b>API 版本（Spring Boot 4 / Spring Framework 7+）：</b>官方已提供一等支持，无需自研拦截器。<br>
 * 在 {@code application.yml} 中配置 {@code spring.mvc.apiversion}（例如请求头、查询参数、路径段、媒体类型参数等解析方式），
 * 控制器上使用 {@code @RequestMapping(version = "1.0")} 或其派生注解的 {@code version} 属性即可。<br>
 * 缺少版本或版本无效时框架抛出 {@link MissingApiVersionException}、{@link InvalidApiVersionException}，
 * 本类将其转为 {@link ApiResult#fail400(String)}，与项目统一返回体一致。
 * </p>
 * <p>
 * 配置示例（请求头携带版本，可按需改用 {@code use.path-segment} / {@code use.query-parameter} 等）：
 * </p>
 * 
 * <pre>
 * spring:
 *   mvc:
 *     apiversion:
 *       required: true
 *       default-version: "1.0"
 *       supported: ["1.0", "2.0"]
 *       use:
 *         header: API-Version
 * </pre>
 *
 * @author leiyangjun
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BizException.class)
	public ResponseEntity<ApiResult<Void>> handleBizException(BizException ex, HttpServletRequest request) {
		HttpStatus status = ex.getHttpStatus();
		ApiResultCustomCode code = ex.getCustomCode();
		if (status.is4xxClientError()) {
			log.warn("[BizException] {} {} http={} bizHint={} msg={}", request.getMethod(), request.getRequestURI(),
					status.value(), code.code(), ex.getMessage());
		} else if (ex.getCause() != null) {
			log.error("[BizException] {} {} http={} bizHint={} msg={}", request.getMethod(), request.getRequestURI(),
					status.value(), code.code(), ex.getMessage(), ex.getCause());
		} else {
			log.error("[BizException] {} {} http={} bizHint={} msg={}", request.getMethod(), request.getRequestURI(),
					status.value(), code.code(), ex.getMessage());
		}
		ApiResult<Void> body;
		if (status == HttpStatus.BAD_REQUEST) {
			body = StringUtils.isNotBlank(ex.getResponseMessage()) ? ApiResult.fail400(code, ex.getResponseMessage())
					: ApiResult.fail400(code);
		} else {
			body = ApiResult.fail500(code);
		}
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String msg = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
				.collect(Collectors.joining("; "));
		String detail = msg.isEmpty() ? "参数校验未通过" : msg;
		log.warn("[MethodArgumentNotValid] {} {} detail={}", request.getMethod(), request.getRequestURI(), detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400(detail));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiResult<Void>> handleBindException(BindException ex, HttpServletRequest request) {
		String msg = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
				.collect(Collectors.joining("; "));
		String detail = msg.isEmpty() ? "参数绑定失败" : msg;
		log.warn("[BindException] {} {} detail={}", request.getMethod(), request.getRequestURI(), detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400(detail));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		String msg = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
				.collect(Collectors.joining("; "));
		log.warn("[ConstraintViolation] {} {} detail={}", request.getMethod(), request.getRequestURI(), msg);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400(msg));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResult<Void>> handleNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		log.warn("[HttpMessageNotReadable] {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400("请求体格式不正确或无法解析"));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		String detail = "缺少必要参数: " + ex.getParameterName();
		log.warn("[MissingParameter] {} {} {}", request.getMethod(), request.getRequestURI(), detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400(detail));
	}

	/**
	 * BaseMapper / Provider 参数配置错误统一按 400 返回；日志在此集中打印，避免底层重复日志。
	 */
	@ExceptionHandler(PersistenceException.class)
	public ResponseEntity<ApiResult<Void>> handlePersistenceException(PersistenceException ex, HttpServletRequest request) {
		String detail = firstNonBlank(ex.getMessage(), "参数校验错误");
		log.warn("[PersistenceException] {} {} detail={}", request.getMethod(), request.getRequestURI(), detail, ex);
		String msg = ApiResultHttp400.UNIFIED_VALIDATE_ERROR.defaultMessage() + "：" + detail;
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResult.fail400(ApiResultHttp400.UNIFIED_VALIDATE_ERROR, msg));
	}

	/**
	 * 已启用 {@code spring.mvc.apiversion} 且要求携带版本，但请求未解析到版本时由框架抛出。
	 */
	@ExceptionHandler(MissingApiVersionException.class)
	public ResponseEntity<ApiResult<Void>> handleMissingApiVersion(MissingApiVersionException ex,
			HttpServletRequest request) {
		String detail = firstNonBlank(ex.getReason(), ex.getMessage(), "缺少 API 版本信息，请按约定传递版本（如请求头、路径、查询参数等）");
		log.warn("[MissingApiVersion] {} {} detail={}", request.getMethod(), request.getRequestURI(), detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400(detail));
	}

	/**
	 * 请求携带的版本不在支持范围内或无法解析时由框架抛出。
	 */
	@ExceptionHandler(InvalidApiVersionException.class)
	public ResponseEntity<ApiResult<Void>> handleInvalidApiVersion(InvalidApiVersionException ex,
			HttpServletRequest request) {
		String detail = firstNonBlank(ex.getReason(), ex.getMessage(), "API 版本无效或不受支持");
		log.warn("[InvalidApiVersion] {} {} version={} detail={}", request.getMethod(), request.getRequestURI(),
				ex.getVersion(), detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail400(detail));
	}

	/**
	 * 浏览器与探测器常访问不存在的静态资源（如 favicon、.well-known）。该类场景按 404 返回并降级为 debug 日志，避免污染错误日志。
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResult<Void>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
		log.debug("[NoResourceFound] {} {} detail={}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResult.fail400("资源不存在"));
	}

	/**
	 * 请求未匹配到任何 Controller Handler 时（开启 no-handler-found 异常化后）统一按 404 返回，并降级为 debug 日志。
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiResult<Void>> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
		log.debug("[NoHandlerFound] {} {} detail={}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResult.fail400("资源不存在"));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResult<Void>> handleAuthenticationException(AuthenticationException ex,
			HttpServletRequest request) {
		log.warn("[AuthenticationException] {} {} msg={}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResult.fail401(ApiResultHttp401.TOKEN_INVALID, ex.getMessage()));
	}

	private static String firstNonBlank(String first, String second, String... fallbacks) {
		if (StringUtils.isNotBlank(first)) {
			return first;
		}
		if (StringUtils.isNotBlank(second)) {
			return second;
		}
		if (fallbacks != null) {
			for (String s : fallbacks) {
				if (StringUtils.isNotBlank(s)) {
					return s;
				}
			}
		}
		return "";
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("[未处理异常] {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResult.fail500(ex));
	}
}

