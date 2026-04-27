package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 排序入参：与 {@link PageVO} 解耦，列表/分页查询可单独传入（可为 {@code null} 表示默认排序策略）。
 *
 * @author leiyangjun
 */
@Data
public class SortVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "排序字段（实体属性名）", example = "createTime")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$", message = "sortName 仅允许字母数字下划线，且必须以字母开头")
	private String sortName;

	@Schema(description = "排序方式：asc/desc（兼容0升序、1降序）", example = "desc")
	@Pattern(regexp = "^(?i)(asc|desc|0|1)$", message = "sortType 仅支持 asc/desc/0/1")
	private String sortType;
}
