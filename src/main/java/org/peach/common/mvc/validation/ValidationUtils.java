package org.peach.common.mvc.validation;

import java.util.Set;

import org.peach.common.mvc.exception.BizException;
import org.peach.common.mvc.result.code.Message400;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * 模型校验工具：仅提供 {@link #valid(Object...)}，对多个对象依次执行 Bean Validation，
 * 遇<b>首条</b>约束违背即抛出 {@link BizException}（码
 * {@link Message400#FIRST_VALIDATE_FAILURE}，{@code data} 为单条说明）。
 * <p>
 * <b>使用方式：</b>{@code ValidationUtils.shared().valid(dto, cmd, vo);}
 * 或注入后调用实例方法。<br>
 * 须由 {@link org.peach.common.mvc.autoconfigure.ValidAutoConfiguration}
 * 注册且在容器刷新之后使用 {@link #shared()}。
 * </p>
 * <p>
 * {@code null} 入参会被跳过，仅校验非空对象。
 * </p>
 *
 * @author leiyangjun
 */
public class ValidationUtils {

	private static volatile ValidationUtils instance;

	private final Validator jakartaValidator;

	public ValidationUtils(Validator jakartaValidator) {
		Assert.notNull(jakartaValidator, "jakartaValidator");
		this.jakartaValidator = jakartaValidator;
	}

	@PostConstruct
	public void registerSharedInstance() {
		ValidationUtils.instance = this;
	}

	public static ValidationUtils shared() {
		ValidationUtils u = instance;
		if (u == null) {
			throw new IllegalStateException(
					"ValidationUtils 尚未就绪：请确认 Spring 容器已刷新、未排除 ValidAutoConfiguration，且存在 jakarta.validation.Validator（由 peach-common-start 传递的 spring-boot-starter-validation 提供）");
		}
		return u;
	}

	/**
	 * 对多个数据模型依次校验；遇首条违背即中断并抛出
	 * {@link BizException#validWarn(org.peach.common.mvc.result.code.MessageCode, String)}。
	 *
	 * @param models 可变参，可传入任意个 DTO / 命令对象等；全部为 {@code null} 或零参时直接返回
	 */
	public void valid(Object... models) {
		if (models == null || models.length == 0) {
			return;
		}
		for (Object model : models) {
			if (model == null) {
				continue;
			}
			Set<ConstraintViolation<Object>> violations = this.jakartaValidator.validate(model);
			if (violations.isEmpty()) {
				continue;
			}
			ConstraintViolation<?> v = violations.iterator().next();
			String label = model.getClass().getSimpleName();
			String line = '[' + label + ']' + v.getPropertyPath() + ": " + v.getMessage();
			throw BizException.validWarn(Message400.FIRST_VALIDATE_FAILURE, line);
		}
	}
}
