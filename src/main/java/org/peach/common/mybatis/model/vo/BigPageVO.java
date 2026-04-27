package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 大数据量游标分页入参：固定按主键升序分页，使用 lastId 作为游标向后翻页。
 *
 * @author leiyangjun
 */
@Data
public class BigPageVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "上一页最后一条记录的主键；首查可不传", example = "1000")
	private Serializable lastId;

	@Schema(description = "每页条数", example = "200")
	@Min(value = 1, message = "pageSize 最小值为1")
	@Max(value = 1000, message = "pageSize 最大值为1000")
	private Integer pageSize = 200;
}

