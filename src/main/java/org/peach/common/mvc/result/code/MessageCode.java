package org.peach.common.mvc.result.code;

/**
 * 统一消息码契约：参与 {@link org.peach.common.mvc.result.ApiResult} 完整 {@code code} 拼接的末段数值与对外 {@code msg} 文案。
 * <p>
 * <b>下游业务微服务</b>须在本服务内自行定义枚举实现本接口，且遵守号段（见 {@link ApiResultCodeRules}）：<br>
 * — 与 HTTP 400 语义绑定的业务码：消息码段须在 {@code 4100–4999}；<br>
 * — 与 HTTP 500 语义绑定的业务码：消息码段须在 {@code 5100–5999}。<br>
 * 启动器内框架内置（如 {@link Message400}、{@link Message401}、{@link org.peach.common.mybatis.code.CrudBizCode} 等）使用保留号段，<b>业务服务不得将其当作自身业务码引用</b>。
 * </p>
 * <p>
 * {@link #frameworkBuiltinMessageCode()} 为 true 时，{@link org.peach.common.mvc.exception.BizException} 路径<b>不做</b>消息码段数值校验；
 * 下游业务枚举须保持默认 false，并接受 {@link ApiResultCodeRules} 中闭区间约束。
 * </p>
 *
 * @author leiyangjun
 */
public interface MessageCode {

	/**
	 * 是否为启动器框架内置消息码（非下游业务服务自定义）。
	 * <p>
	 * 仅框架内置枚举应覆盖为 {@code true}；业务服务实现的枚举禁止覆盖为 {@code true} 以绕过号段约束。
	 * </p>
	 */
	default boolean frameworkBuiltinMessageCode() {
		return false;
	}

	/** 参与完整 {@code code} 拼接的消息码段数值。 */
	int code();

	/** 对外展示说明（须与枚举常量绑定）。 */
	String msg();
}
