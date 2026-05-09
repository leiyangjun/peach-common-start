package org.peach.common.mybatis.code;

import org.peach.common.mvc.exception.BizException;
import org.peach.common.mvc.result.code.ApiResultCustomCode;

/**
 * 通用 CRUD / BaseMapper Provider 等业务码与文案统一入口（配合 {@link BizException}）。
 * <p>
 * 提示段末两位须大于 20；带占位符的 {@link #msg} 使用 {@link #badRequestFormatted(Object...)} 生成对外说明。
 * </p>
 *
 * @author leiyangjun
 */
public enum CrudBizCode implements ApiResultCustomCode {

	/** 按主键未查到记录 */
	RECORD_NOT_FOUND(4021, "记录不存在"),

	/** 唯一性冲突（如 @Unique 字段重复） */
	DUPLICATE_RECORD(4022, "数据已存在"),

	/** 更新/删除影响行数为 0（可能记录不存在或并发变更） */
	AFFECTED_ZERO(4023, "操作未生效，记录可能不存在或已变更"),

	/** 主键缺失或无效（@ID 与入参） */
	TABLE_KEY_INVALID(4024, "主键缺失或无效，请检查 @ID 与入参"),

	/** 逻辑删除列或主键配置无效 */
	LOGIC_DELETE_CONFIG_INVALID(4025, "逻辑删除字段或主键配置无效"),

	/** 实体上注解冲突（多个 @ID / @LogicDelete 等） */
	ENTITY_ANNOTATION_CONFLICT(4026, "表字段注解配置冲突（如同类上多个 @ID / @LogicDelete）"),

	/** 按唯一键查询时条件值为 null */
	UNIQUE_QUERY_VALUE_REQUIRED(4027, "按唯一键查询时，uniqueValue 不能为 null"),

	/** voClass 上缺少 @Unique */
	UNIQUE_FIELD_REQUIRED_ON_VO(4028, "按唯一键查询时，voClass 须包含至少一个 @Unique 字段"),

	/** voClass 上存在多个 @Unique */
	UNIQUE_FIELD_MULTIPLE_ON_VO(4029, "按唯一键查询时，仅允许一个 @Unique 字段"),

	/** 实体上存在多个 @Range；msg 模板参数为数量 */
	RANGE_FIELD_MULTIPLE(4030, "仅允许一个 @Range 字段，当前数量=%d"),

	/** 排序字段名不合法；msg 模板参数为 sortName */
	SORT_FIELD_PATTERN_INVALID(4031, "排序字段非法：%s"),

	/** 排序字段不在映射列中；msg 模板参数为 sortName */
	SORT_FIELD_NOT_MAPPED(4032, "排序字段不存在：%s"),

	/** 排序方式非法；msg 模板参数为 sortType */
	SORT_TYPE_INVALID(4033, "排序方式非法：%s"),

	/** 游标分页参数缺失 */
	CURSOR_PAGE_REQUIRED(4034, "bigPage 不能为空"),

	/** 游标分页 pageSize 非法 */
	CURSOR_PAGE_SIZE_INVALID(4035, "bigPage.pageSize 不能为空且必须大于0");

	private final int code;
	private final String msg;

	CrudBizCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	@Override
	public int code() {
		return code;
	}

	@Override
	public String msg() {
		return msg;
	}

	/**
	 * 无占位符时直接使用枚举文案抛出 {@link BizException#badRequest(ApiResultCustomCode)}。
	 */
	public BizException badRequest() {
		return BizException.badRequest(this);
	}

	/**
	 * 将 {@link #msg} 作为 {@link String#format(String, Object...)} 模板格式化后抛出
	 * {@link BizException#badRequest(ApiResultCustomCode, String)}。
	 */
	public BizException badRequestFormatted(Object... args) {
		return BizException.badRequest(this, String.format(this.msg, args));
	}
}
