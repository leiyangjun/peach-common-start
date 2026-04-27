package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页入参（页码、页大小）；排序请使用 {@link SortVO}。
 *
 * @author leiyangjun
 */
@Data
public class PageVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "页码（从1开始）", example = "1")
	@Min(value = 1, message = "pageNum 最小值为1")
	private Integer pageNum = 1;

	@Schema(description = "每页条数", example = "20")
	@Min(value = 1, message = "pageSize 最小值为1")
	@Max(value = 1000, message = "pageSize 最大值为1000")
	private Integer pageSize = 20;
}
