package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用单字段区间查询入参：实体上须<strong>恰好一个</strong>字段标注 {@link org.peach.common.mybatis.annotation.Range}，
 * 本对象提供该字段的下界（{@code >=}）与上界（{@code <=}）；任一端为 {@code null} 则不生成对应条件。
 * <p>
 * 与 {@link SearchVO} 解耦，分页接口可单独传区间而不携带关键字参数。
 * </p>
 *
 * @author leiyangjun
 */
@Data
public class RangeVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "范围起始值（>=），对应 @Range 标注列")
	private Object startValue;

	@Schema(description = "范围结束值（<=），对应 @Range 标注列")
	private Object endValue;
}
