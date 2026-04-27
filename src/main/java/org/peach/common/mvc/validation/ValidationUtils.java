package org.peach.common.mvc.validation;

import java.util.Set;

import org.peach.common.mvc.exception.BizException;
import org.peach.common.mvc.result.code.ModelValidateBizCode;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * 模型校验工具：仅提供 {@link #valid(Object...)}，对多个对象依次执行 Bean Validation，
 * 若有违背则抛出 {@link BizException}（业务码 {@link ModelValidateBizCode#AGGREGATE}，{@code msg} 为聚合明细）。
 * <p>
 * <b>使用方式：</b>{@code ValidationUtils.shared().valid(dto, cmd, vo);} 或注入后调用实例方法。<br>
 * 须由 {@link org.peach.common.mvc.autoconfigure.ValidAutoConfiguration} 注册且在容器刷新之后使用 {@link #shared()}。
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
					"ValidationUtils 尚未就绪：请确认已引入 spring-boot-starter-validation、Spring 容器已刷新，且未排除 ValidAutoConfiguration");
		}
		return u;
	}

	/**
	 * 对多个数据模型依次校验；任一存在约束违背则抛出 {@link BizException#badRequest(org.peach.common.mvc.result.code.ApiResultCustomCode, String)}，
	 * {@code msg} 为所有违背信息的聚合（带类型简名与属性路径便于区分）。
	 *
	 * @param models 可变参，可传入任意个 DTO / 命令对象等；全部为 {@code null} 或零参时直接返回
	 */
	public void valid(Object... models) {
		if (models == null || models.length == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (Object model : models) {
			if (model == null) {
				continue;
			}
			Set<ConstraintViolation<Object>> violations = this.jakartaValidator.validate(model);
			if (violations.isEmpty()) {
				continue;
			}
			String label = model.getClass().getSimpleName();
			for (ConstraintViolation<?> v : violations) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append('[').append(label).append(']').append(v.getPropertyPath()).append(": ").append(v.getMessage());
			}
		}
		if (sb.length() > 0) {
			throw BizException.badRequest(ModelValidateBizCode.AGGREGATE, sb.toString());
		}
	}
}

