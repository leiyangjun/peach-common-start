package org.peach.common.mybatis.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用出参父类（VO），用于统一接口字段描述。
 *
 * @author leiyangjun
 */
@Data
public class BaseVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@Schema(description = "创建人ID")
	private String creatorId;

	@Schema(description = "更新人ID")
	private String editorId;
}
