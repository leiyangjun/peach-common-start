package org.peach.common.mybatis.code;

import org.peach.common.mvc.result.code.ApiResultCustomCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用 CRUD / 持久化相关业务码（用于 {@link org.peach.common.mvc.exception.BizException}），
 * 提示段末两位须大于 20。
 *
 * @author leiyangjun
 */
@Getter
@RequiredArgsConstructor
public enum CrudBizCode implements ApiResultCustomCode {

	/** 按主键未查到记录 */
	RECORD_NOT_FOUND(4021, "记录不存在"),

	/** 唯一性冲突（如 @Unique 字段重复） */
	DUPLICATE_RECORD(4022, "数据已存在"),

	/** 更新/删除影响行数为 0（可能记录不存在或并发变更） */
	AFFECTED_ZERO(4023, "操作未生效，记录可能不存在或已变更");

	private final int code;
	private final String msg;

	@Override
	public int code() {
		return code;
	}

	@Override
	public String msg() {
		return msg;
	}
}
