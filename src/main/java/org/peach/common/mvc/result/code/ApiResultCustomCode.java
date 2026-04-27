package org.peach.common.mvc.result.code;

/**
 * 业务自定义错误码约定（通常由各服务枚举实现），供 {@link org.peach.common.mvc.result.ApiResult#fail400(ApiResultCustomCode)}、
 * {@link org.peach.common.mvc.result.ApiResult#fail500(ApiResultCustomCode)} 使用。
 * <p>
 * {@link #code()} 表示参与完整 {@code code} 拼接的<b>四位提示编码段</b>（与框架 {@link ApiResultCodeSpec#hintCode()} 同段位）；<br>
 * 该数值的<b>末两位（十进制）须严格大于 20</b>，末两位 ≤ 20 的号段由框架保留（如 2001、4003、5001 等）。<br>
 * 示例：{@code 5021}「余额不足」——末两位为 21。
 * </p>
 *
 * @author leiyangjun
 */
public interface ApiResultCustomCode {

	/** 四位提示编码段的数值，末两位须 &gt; 20。 */
	int code();

	/** 对外展示的错误说明。 */
	String msg();
}

