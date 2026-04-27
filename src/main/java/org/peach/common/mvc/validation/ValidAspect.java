package org.peach.common.mvc.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
 * <b>一、切面能否「帮方法补上 BindingResult」？</b><br>
 * <b>不能。</b>{@link BindingResult}（及 {@link Errors}）必须由 <b>Controller 方法形参列表</b>显式声明，
 * Spring MVC 在绑定 {@code @ModelAttribute} / 部分 {@code @RequestBody} 场景下，
 * 会按约定把校验错误写入紧跟在目标参数之后的 {@code BindingResult}。
 * AOP 只能拦截调用，无法改变方法签名，也无法向 DispatcherServlet 的入参解析流程里「插入」一个 BindingResult。
 * 若你需要在方法内部根据 {@code bindingResult.hasErrors()} 分支处理，请继续使用传统写法，例如：
 * </p>
 * 
 * <pre>
 * public ApiResult&lt;?&gt; postClient(&#64;RequestBody &#64;Validated ClientPostDto dto, BindingResult bindingResult) {
 *     if (bindingResult.hasErrors()) {
 *         return ApiResult.fail400(bindingResult.getFieldError().getDefaultMessage());
 *     }
 *     ...
 * }
 * </pre>
 * 
 * <p>
 * 本切面走的是另一条路径：<b>无 {@code BindingResult} 形参、且不想在每个参数上写 {@code @Valid}</b> 时，
 * 在进入方法前完成校验；失败时抛出 {@link MethodArgumentNotValidException}，
 * 与 Spring 默认「{@code @Valid} + 无 BindingResult」行为一致，便于 {@link org.peach.common.mvc.exception.GlobalExceptionHandler}
 * 统一返回 {@code ApiResult.fail400}。
 * </p>
 * 
 * <p>
 * <b>二、是否可以「不添加 {@code @Validated} / {@code @Valid}」？</b><br>
 * 在类上加了 {@link AutoValidated} 的前提下，对符合条件的 {@code @RequestBody} 参数，<b>可以不在参数上写
 * {@code @Valid}/{@code @Validated}</b>，由本切面调用 {@link SmartValidator} 完成校验。
 * 若参数上已经写了 {@code @Valid} 或 {@code @Validated}，则认为由 Spring MVC 参数解析器负责校验，
 * <b>切面会跳过该参数</b>，避免与默认机制重复执行。
 * </p>
 * 
 * <p>
 * <b>三、何时跳过切面校验？</b>
 * </p>
 * <ul>
 * <li>下一形参类型为 {@link BindingResult}（或 {@link Errors}）时：符合
 * 「{@code (Dto dto, BindingResult br)}」约定，交由 Spring 绑定/校验流程处理，切面不参与。</li>
 * <li>参数上已有 {@link Valid} 或 {@link Validated}。</li>
 * <li>{@link AutoValidated#requestBody()} 为 {@code false} 时，本实现仍可按该开关扩展；当前版本仅当注解属性为
 * {@code true}（默认）时对 {@code @RequestBody} 生效。</li>
 * </ul>
 *
 * @author leiyangjun
 * @see ValidationUtils
 * @see BindingResult
 */
@Aspect
public class ValidAspect {

	private static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

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
				// 与 Spring MVC 约定一致：若紧随 BindingResult，则由框架填充错误，不在此处重复校验
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
				MethodParameter mp = new MethodParameter(method, i);
				mp.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);
				throw new MethodArgumentNotValidException(mp, errors);
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

	/**
	 * 经典写法：{@code (@RequestBody Dto dto, BindingResult br)} —— BindingResult 必须紧挨被校验参数之后。
	 */
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

