package org.peach.common.mvc.result.code;

/**
 * 业务码中间段（HTTP 语义族）与末段（提示编码）的约定，供 {@link org.peach.common.mvc.result.ApiResult} 拼接完整 {@code code}。
 *
 * @author leiyangjun
 */
public interface ApiResultCodeSpec {

	/** HTTP 语义族，固定三位参与 code 拼接（如 200、400、401）。 */
	int family();

	/** 提示信息编码，固定四位参与 code 拼接（如 2001、4001）。 */
	int hintCode();

	/** 默认提示文案；可被具体接口覆盖（如校验信息、异常信息）。 */
	String defaultMessage();
}

