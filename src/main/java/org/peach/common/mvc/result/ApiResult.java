package org.peach.common.mvc.result;

import org.peach.common.mvc.autoconfigure.ModuleCodeCache;
import org.peach.common.mvc.result.code.Message200;
import org.springframework.http.HttpStatus;

/**
 * 统一 API 成功返回体：{@link #code}、{@link #msg}、{@link #data}。
 * <p>
 * 下游业务仅应使用 {@link #ok()}、{@link #ok(Object)}；非 200 装配由同模块内
 * {@link org.peach.common.mvc.exception.ErrorResult} 完成。
 * </p>
 *
 * @param <T> 数据体类型
 * @author leiyangjun
 */
public class ApiResult<T> {

	private final String code;
	private String msg;
	private final T data;

	private ApiResult(String code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	/**
	 * 将框架层错误体转为与成功响应一致的对外类型（{@code data} 恒为 {@code null}），仅供启动器内
	 * {@link org.peach.common.mvc.exception.ErrorResult} 使用。
	 */
//	public static ApiResult<Void> ofErrorBody(String code, String msg) {
//		return new ApiResult<>(code, msg, null);
//	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	/**
	 * @Title: ok
	 * @Description: 用于Controller返回消息体 带data 通常用于携带数据的操作成功
	 */
	public static <T> ApiResult<T> ok(T data) {
		Message200 ok = Message200.OK;
		return new ApiResult<>(ModuleCodeCache.get() + HttpStatus.OK.value() + ok.code(), ok.msg(), data);
	}

	/**
	 * @Title: ok
	 * @Description: 用于Controller返回消息体 不带data通常用于提示操作成功
	 */
	public static ApiResult<Void> ok() {
		return ok(null);
	}
}
