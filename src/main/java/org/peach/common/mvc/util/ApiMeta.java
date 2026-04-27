package org.peach.common.mvc.util;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 本服务对外暴露的 API 元信息（用于动态权限、定时任务绑定 API 等）。
 * <p>
 * 使用普通 JavaBean + {@link Schema}，便于 SpringDoc/OpenAPI 生成文档，后续也可按需扩展字段。
 * </p>
 *
 * @author leiyangjun
 */
@Data
public class ApiMeta implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "HTTP 方法，如 GET、POST；未在映射中限定方法时可能为 ALL")
	private String method;

	@Schema(description = "接口摘要（@Operation.summary），列表中仅包含摘要非空的接口")
	private String summary;

	@Schema(description = "接口详细说明，来自 Swagger @Operation.description，无注解时可能为空")
	private String description;

	@Schema(description = "展示用简短说明，与 summary 一致")
	private String apiDesc;

	@Schema(description = "路径模板（与 pathPattern 相同），如 /api/users/{id}")
	private String urlPath;

	@Schema(description = "PathPattern 表达式字符串，供 PathPatternParser 做路径匹配")
	private String pathPattern;

	@Schema(description = "处理器定位：全限定类名#方法名")
	private String handler;
}
