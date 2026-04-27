package org.peach.common.mvc.result;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.result.code.ApiResultCodeRules;
import org.peach.common.mvc.result.code.ApiResultCodeSpec;
import org.peach.common.mvc.result.code.ApiResultCustomCode;
import org.peach.common.mvc.result.code.ApiResultHttp200;
import org.peach.common.mvc.result.code.ApiResultHttp400;
import org.peach.common.mvc.result.code.ApiResultHttp401;
import org.peach.common.mvc.result.code.ApiResultHttp403;
import org.peach.common.mvc.result.code.ApiResultHttp500;

import lombok.Getter;

/**
 * 统一 API 返回体：仅包含 {@link #code}、{@link #msg}、{@link #data}（已去掉独立 {@code status}
 * 字段）。
 * <p>
 * {@link #code} 由三段拼接：<b>模块编码（四位，来自 {@code spring.application.module-code}）</b>
 * + <b>HTTP 语义族（三位，如 200、401）</b> + <b>提示编码（四位，如 2001、4001）</b>。<br>
 * 示例：模块 {@code B001}、族 {@code 200}、提示 {@code 2001} →
 * {@code code=B0012002001}，{@code msg=操作成功}。
 * </p>
 * <p>
 * 模块编码由启动时自动配置注入，调用方无需传入；请使用静态工厂方法构造实例（构造私有）。
 * </p>
 *
 * @param <T> 数据体类型；无数据体时使用 {@link Void} 且 {@link #data} 为 {@code null}
 * @author leiyangjun
 */
@Getter
public final class ApiResult<T> {

	private static volatile String moduleCode;

	private final String code;
	private final String msg;
	private final T data;

	private ApiResult(String code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	/**
	 * 由自动配置在启动阶段注入模块编码（四位）。未配置时不提供默认值，调用返回构造会抛异常。
	 */
	public static void setModuleCode(String fourChars) {
		Objects.requireNonNull(fourChars, "moduleCode");
		String t = fourChars.trim();
		if (t.length() != 4) {
			throw new IllegalArgumentException("spring.application.module-code 必须为四位字符串，当前长度=" + t.length());
		}
		moduleCode = t;
	}

	private static String requiredModuleCode() {
		String mc = moduleCode;
		if (mc == null || mc.isBlank()) {
			throw new IllegalStateException("未初始化模块编码：请配置 spring.application.module-code（四位字符串）");
		}
		return mc;
	}

	private static String composeFullCode(ApiResultCodeSpec spec) {
		Objects.requireNonNull(spec, "spec");
		return composeFullCode(spec.family(), spec.hintCode());
	}

	private static String composeFullCode(int family, int hintCode) {
		int fam = family % 1000;
		if (fam < 0) {
			fam = -fam % 1000;
		}
		int hint = Math.abs(hintCode) % 10000;
		return requiredModuleCode() + String.format("%03d", fam) + String.format("%04d", hint);
	}

	private static <T> ApiResult<T> fromSpec(ApiResultCodeSpec spec, String msg, T data) {
		Objects.requireNonNull(spec, "spec");
		String m = msg != null ? msg : spec.defaultMessage();
		return new ApiResult<>(composeFullCode(spec), m, data);
	}

	private static <T> ApiResult<T> fromCustom(int httpFamily, ApiResultCustomCode custom, T data) {
		Objects.requireNonNull(custom, "custom");
		ApiResultCodeRules.assertCustomBizHintTail(custom.code());
		String m = Objects.requireNonNull(custom.msg(), "custom.msg");
		return new ApiResult<>(composeFullCode(httpFamily, custom.code()), m, data);
	}

	/**
	 * 成功且无返回数据体。
	 */
	public static ApiResult<Void> ok() {
		return fromSpec(ApiResultHttp200.OK, null, null);
	}

	/**
	 * 成功并携带数据体。
	 */
	public static <T> ApiResult<T> ok(T data) {
		return fromSpec(ApiResultHttp200.OK, null, data);
	}

	/**
	 * 400：参数非法，{@code msg} 一般为校验框架返回的详细信息。
	 */
	public static <T> ApiResult<T> fail400(String validationMessage) {
		return fromSpec(ApiResultHttp400.INVALID_PARAM, Objects.requireNonNull(validationMessage, "validationMessage"),
				null);
	}

	/**
	 * 400：使用指定的 400 语义码；{@code responseMessage} 非空时作为对外 msg，否则使用枚举默认文案。
	 */
	public static <T> ApiResult<T> fail400(ApiResultHttp400 code, String responseMessage) {
		Objects.requireNonNull(code, "code");
		String m = StringUtils.isNotBlank(responseMessage) ? responseMessage : code.defaultMessage();
		return new ApiResult<>(composeFullCode(code), Objects.requireNonNull(m), null);
	}

	/**
	 * 400：业务自定义错误（如参数/业务规则不满足），使用实现 {@link ApiResultCustomCode} 的枚举等对象；
	 * {@link ApiResultCustomCode#code()} 末两位须大于 20。
	 */
	public static <T> ApiResult<T> fail400(ApiResultCustomCode custom) {
		return fromCustom(400, custom, null);
	}

	/**
	 * 400：同上，但 {@code responseMessage} 非空时作为对外 {@code msg}（如聚合校验明细），否则使用 {@code custom.msg()}。
	 */
	public static <T> ApiResult<T> fail400(ApiResultCustomCode custom, String responseMessage) {
		Objects.requireNonNull(custom, "custom");
		ApiResultCodeRules.assertCustomBizHintTail(custom.code());
		String m = StringUtils.isNotBlank(responseMessage) ? responseMessage : custom.msg();
		return new ApiResult<>(composeFullCode(400, custom.code()), Objects.requireNonNull(m), null);
	}

	/**
	 * 401：用户名或密码错误（固定枚举文案与编码）。
	 */
	public static <T> ApiResult<T> fail401() {
		return fromSpec(ApiResultHttp401.USERNAME_OR_PASSWORD, null, null);
	}

	/**
	 * 401：使用指定 {@link ApiResultHttp401} 语义（如令牌无效）。
	 */
	public static <T> ApiResult<T> fail401(ApiResultHttp401 spec) {
		return fromSpec(Objects.requireNonNull(spec, "spec"), null, null);
	}

	/**
	 * 401：使用指定语义，并覆盖对外 {@code msg}。
	 */
	public static <T> ApiResult<T> fail401(ApiResultHttp401 spec, String responseMessage) {
		Objects.requireNonNull(spec, "spec");
		String m = StringUtils.isNotBlank(responseMessage) ? responseMessage : spec.defaultMessage();
		return new ApiResult<>(composeFullCode(spec), m, null);
	}

	/**
	 * 403：没有访问权限。
	 */
	public static <T> ApiResult<T> fail403() {
		return fromSpec(ApiResultHttp403.NO_ACCESS, null, null);
	}

	/**
	 * 500：系统内部错误，使用指定文案（通常为异常信息）。
	 */
	public static <T> ApiResult<T> fail500(String message) {
		String m = StringUtils.defaultIfBlank(message, ApiResultHttp500.INTERNAL.defaultMessage());
		return fromSpec(ApiResultHttp500.INTERNAL, m, null);
	}

	/**
	 * 500：系统内部错误，优先使用 {@link Throwable#getMessage()}，为空则使用枚举默认文案。
	 */
	public static <T> ApiResult<T> fail500(Throwable throwable) {
		String m = throwable == null ? null : throwable.getMessage();
		return fail500(m);
	}

	/**
	 * 500：业务自定义系统/服务类错误（如余额不足），使用实现 {@link ApiResultCustomCode} 的枚举等对象；
	 * {@link ApiResultCustomCode#code()} 末两位须大于 20。
	 */
	public static <T> ApiResult<T> fail500(ApiResultCustomCode custom) {
		return fromCustom(500, custom, null);
	}
}

