package org.peach.common.mvc.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.peach.common.mvc.exception.BizException;
import org.peach.common.mvc.result.code.Message400;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 统一参数校验切面：对标注了 {@link AutoValidated} 的 {@link RestController}，
 * 在进入目标方法前对「仅带 {@link RequestBody}、且未配合 Spring 原生 {@link BindingResult} 形参、
 * 且未再标 {@link Valid}/{@link Validated}」的对象参数执行 Bean Validation。
 * <p>
 * 校验失败时直接抛出 {@link BizException}（首条字段错误，与 {@link ValidationUtils} 语义一致）。
 * </p>
 *
 * @author leiyangjun
 * @see ValidationUtils
 * @see BindingResult
 */
@Aspect
public class ValidAspect {

	private final SmartValidator smartValidator;

	public ValidAspect(SmartValidator smartValidator) {
		this.smartValidator = smartValidator;
	}

	/**
	 * 类上需同时存在 {@link RestController} 与 {@link AutoValidated}，避免误切 Service 等非 Web 入口。
	 */
	@Around("@within(org.springframework.web.bind.annotation.RestController) && @within(org.peach.common.mvc.validation.AutoValidated)")
	public Object aroundRestController(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Object[] args = joinPoint.getArgs();
		Class<?>[] paramTypes = method.getParameterTypes();
		Annotation[][] paramAnns = method.getParameterAnnotations();

		Class<?> controllerType = method.getDeclaringClass();
		AutoValidated autoValidated = controllerType.getAnnotation(AutoValidated.class);
		boolean validateBody = autoValidated == null || autoValidated.requestBody();

		for (int i = 0; i < paramTypes.length; i++) {
			if (shouldSkipParameterType(paramTypes[i])) {
				continue;
			}
			if (!validateBody || !hasRequestBody(paramAnns[i])) {
				continue;
			}
			if (hasBindingStylePartner(paramTypes, i)) {
				continue;
			}
			if (hasExplicitBeanValidationAnnotation(paramAnns[i])) {
				continue;
			}
			Object arg = args[i];
			if (arg == null) {
				continue;
			}
			BeanPropertyBindingResult errors = new BeanPropertyBindingResult(arg, "requestBody");
			this.smartValidator.validate(arg, errors);
			if (errors.hasErrors()) {
				FieldError fe = errors.getFieldErrors().get(0);
				String line = fe.getField() + ": " + fe.getDefaultMessage();
				throw BizException.validWarn(Message400.METHOD_ARG_NOT_VALID, line);
			}
		}
		return joinPoint.proceed();
	}

	private static boolean shouldSkipParameterType(Class<?> type) {
		return BindingResult.class.isAssignableFrom(type) || Errors.class.isAssignableFrom(type)
				|| HttpServletRequest.class.isAssignableFrom(type) || HttpServletResponse.class.isAssignableFrom(type)
				|| MultipartFile.class.isAssignableFrom(type) || MultipartFile[].class.isAssignableFrom(type);
	}

	private static boolean hasRequestBody(Annotation[] anns) {
		for (Annotation a : anns) {
			if (a instanceof RequestBody) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasBindingStylePartner(Class<?>[] paramTypes, int requestBodyIndex) {
		int next = requestBodyIndex + 1;
		if (next >= paramTypes.length) {
			return false;
		}
		return BindingResult.class.isAssignableFrom(paramTypes[next]) || Errors.class.isAssignableFrom(paramTypes[next]);
	}

	private static boolean hasExplicitBeanValidationAnnotation(Annotation[] anns) {
		for (Annotation a : anns) {
			if (a instanceof Valid || a instanceof Validated) {
				return true;
			}
		}
		return false;
	}
}
