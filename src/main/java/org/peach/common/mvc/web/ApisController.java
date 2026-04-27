package org.peach.common.mvc.web;

import java.util.List;

import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mvc.util.ApiMappingUtil;
import org.peach.common.mvc.util.ApiMeta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 提供本服务 Spring MVC 映射列表，供动态权限、Quartz 等平台化配置拉取。
 *
 * @author leiyangjun
 */
@Tag(name = "API 目录", description = "查询本服务已注册的 HTTP 接口映射")
@RestController
public class ApisController {

	/**
	 * 按 HTTP 方法过滤，可选按路径或说明关键字模糊匹配。
	 */
	@Operation(summary = "查询 API 映射列表(仅扫描带有Operation描述的API且summary不为空，其他接口忽略)", description = "method 必填；keyword 可选，匹配 pathPattern/urlPath、summary、description、handler")
	@GetMapping("/apis")
	public ApiResult<List<ApiMeta>> listApis(
			@Parameter(description = "HTTP 方法，如 GET、POST（大小写不敏感）", required = true) @RequestParam String method,
			@Parameter(description = "模糊关键字，匹配路径或接口说明") @RequestParam(required = false) String keyword) {
		return ApiResult.ok(ApiMappingUtil.searchApis(method, keyword));
	}
}
