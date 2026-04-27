package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用查询入参：模糊搜索 + 单区间查询。
 *
 * @author leiyangjun
 */
@Data
public class CommonQueryVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "模糊搜索关键字，会作用于实体中标记 @SearchValue 的字段")
	private String searchValue;

	@Schema(description = "范围起始值（>=）")
	private Object startValue;

	@Schema(description = "范围结束值（<=）")
	private Object endValue;
}

