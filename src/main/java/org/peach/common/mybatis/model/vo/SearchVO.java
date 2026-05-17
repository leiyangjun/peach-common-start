package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用关键字模糊查询入参：与实体上 {@link org.peach.common.mybatis.annotation.SearchValue} 标注列组合生成 {@code LIKE} 条件。
 * <p>
 * 与 {@link RangeVO} 解耦，关键字模糊与单字段区间可独立组合分页入参。
 * </p>
 *
 * @author leiyangjun
 */
@Data
public class SearchVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "模糊搜索关键字，作用于实体中标记 @SearchValue 的字段（OR 连接）")
	private String searchValue;
}
