package org.peach.common.mvc.exception;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.result.code.Message400;
import org.peach.common.mvc.result.code.Message401;
import org.peach.common.mvc.result.code.Message403;
import org.peach.common.mvc.result.code.Message404;
import org.peach.common.mvc.result.code.Message500;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理：返回
 * {@link org.springframework.http.ResponseEntity}{@code <ErrorResult>}，响应体由
 * Jackson 序列化； 日志中打印 {@link ErrorResult#toString()} 仅为简要文本，与响应 JSON 无关联。
 *
 * @author leiyangjun
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BizException.class)
	public ResponseEntity<ErrorResult> handleBizException(BizException ex, HttpServletRequest request) {
		HttpStatus status = ex.getHttpStatus();
		ErrorResult errorResult = ex.getErrorResult();
		if (status == HttpStatus.BAD_REQUEST) {
			log.warn("[BizException] {} {} http={} error info={}", request.getMethod(), request.getRequestURI(),
					status.value(), errorResult);
		} else {
			log.error("[BizException] {} {} http={} error info={}", request.getMethod(), request.getRequestURI(),
					status.value(), errorResult, ex);
		}
		return ResponseEntity.status(status).body(errorResult);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResult> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String detail = firstFieldErrorMessage(ex.getBindingResult());
		ErrorResult errorResult = ErrorResult.validWarn(Message400.METHOD_ARG_NOT_VALID, detail);
		log.warn("[MethodArgumentNotValid] {} {} detail={} error info={}", request.getMethod(), request.getRequestURI(),
				detail, errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResult> handleBindException(BindException ex, HttpServletRequest request) {
		String detail = firstFieldErrorMessage(ex.getBindingResult());
		ErrorResult errorResult = ErrorResult.validWarn(Message400.BIND_FAILURE, detail);
		log.warn("[BindException] {} {} detail={} error info={}", request.getMethod(), request.getRequestURI(), detail,
				errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResult> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		String detail = firstConstraintLine(ex);
		ErrorResult errorResult = ErrorResult.validWarn(Message400.CONSTRAINT_VIOLATION, detail);
		log.warn("[ConstraintViolation] {} {} detail={} error info={}", request.getMethod(), request.getRequestURI(),
				detail, errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResult> handleNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.validWarn(Message400.BODY_NOT_READABLE);
		log.warn("[HttpMessageNotReadable] {} {} msg={} error info={}", request.getMethod(), request.getRequestURI(),
				ex.getMessage(), errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResult> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.validWarn(Message400.MISSING_PARAMETER);
		log.warn("[MissingParameter] {} {} name={} error info={}", request.getMethod(), request.getRequestURI(),
				ex.getParameterName(), errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResult> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.error(Message500.INTERNAL);
		log.error("[DataAccessException] {} {} error info={}", request.getMethod(), request.getRequestURI(),
				errorResult, ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
	}

	@ExceptionHandler(MissingApiVersionException.class)
	public ResponseEntity<ErrorResult> handleMissingApiVersion(MissingApiVersionException ex,
			HttpServletRequest request) {
		String detail = firstNonBlank(ex.getReason(), ex.getMessage(), Message400.MISSING_API_VERSION.msg());
		ErrorResult errorResult = ErrorResult.validWarn(Message400.MISSING_API_VERSION, detail);
		log.warn("[MissingApiVersion] {} {} detail={} error info={}", request.getMethod(), request.getRequestURI(),
				detail, errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(InvalidApiVersionException.class)
	public ResponseEntity<ErrorResult> handleInvalidApiVersion(InvalidApiVersionException ex,
			HttpServletRequest request) {
		String detail = firstNonBlank(ex.getReason(), ex.getMessage(), Message400.INVALID_API_VERSION.msg());
		ErrorResult errorResult = ErrorResult.validWarn(Message400.INVALID_API_VERSION, detail);
		log.warn("[InvalidApiVersion] {} {} version={} detail={} error info={}", request.getMethod(),
				request.getRequestURI(), ex.getVersion(), detail, errorResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResult> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.notFound(Message404.NOT_FOUND);
		log.debug("[NoResourceFound] {} {} detail={} error info={}", request.getMethod(), request.getRequestURI(),
				ex.getMessage(), errorResult);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResult);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ErrorResult> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.notFound(Message404.NOT_FOUND);
		log.debug("[NoHandlerFound] {} {} detail={} error info={}", request.getMethod(), request.getRequestURI(),
				ex.getMessage(), errorResult);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResult);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResult> handleAuthenticationException(AuthenticationException ex,
			HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.unauthorized(Message401.TOKEN_INVALID);
		log.warn("[AuthenticationException] {} {} msg={} error info={}", request.getMethod(), request.getRequestURI(),
				ex.getMessage(), errorResult);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResult);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResult> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.forbidden(Message403.NO_ACCESS);
		log.warn("[AccessDenied] {} {} msg={} error info={}", request.getMethod(), request.getRequestURI(),
				ex.getMessage(), errorResult);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResult);
	}

	private static String firstFieldErrorMessage(BindingResult br) {
		if (br == null || !br.hasErrors()) {
			return null;
		}
		FieldError fe = br.getFieldErrors().get(0);
		return fe.getField() + ": " + fe.getDefaultMessage();
	}

	private static String firstConstraintLine(ConstraintViolationException ex) {
		if (ex.getConstraintViolations() == null || ex.getConstraintViolations().isEmpty()) {
			return null;
		}
		ConstraintViolation<?> v = ex.getConstraintViolations().iterator().next();
		return v.getPropertyPath() + ": " + v.getMessage();
	}

//	/**
//	 * 将首条说明格式进枚举模板；无明细时用占位避免裸 {@code %s}。
//	 */
//	private static String formatMessage400(Message400 code, String detail) {
//		String d = StringUtils.isNotBlank(detail) ? detail : "—";
//		return String.format(code.msg(), d);
//	}

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
	public ResponseEntity<ErrorResult> handleUnexpected(Exception ex, HttpServletRequest request) {
		ErrorResult errorResult = ErrorResult.error(Message500.INTERNAL);
		log.error("[未处理异常] {} {} error info={}", request.getMethod(), request.getRequestURI(), errorResult, ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
	}
}
