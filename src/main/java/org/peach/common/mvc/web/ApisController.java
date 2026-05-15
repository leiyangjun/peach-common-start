package org.peach.common.mvc.web;

import java.util.List;

import org.peach.common.mvc.api.context.annotation.AdminApi;
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
 * <p>
 * 形态相关说明：
 * </p>
 * <ul>
 * <li>{@code GET /apis}：支持可选查询参数
 * {@code apiType}（admin/app/openapi），与下方三条路径使用同一注册表数据源。</li>
 * <li>{@code GET /apis/type/admin}、{@code GET /apis/type/app}、{@code GET /apis/type/openapi}：按形态返回元数据列表，与
 * {@code /apis?apiType=} 使用同一注册表数据源。</li>
 * </ul>
 *
 * @author leiyangjun
 */
@Tag(name = "API 目录", description = "查询本服务已注册的 HTTP 接口映射")
@RestController
@AdminApi
public class ApisController {

	/**
	 * 按 HTTP 方法过滤，可选按路径或说明关键字模糊匹配，可选按接口形态（admin/app/openapi）过滤。
	 */
	@Operation(summary = "查询 API 映射列表(仅扫描带有Operation描述的API且summary不为空，其他接口忽略)", description = "method 必填；keyword、apiType 可选；apiType 匹配注册表中的形态字段（来自 MVC 扫描，无注解时默认 admin）")
	@GetMapping("/apis")
	public ApiResult<List<ApiMeta>> listApis(
			@Parameter(description = "HTTP 方法，如 GET、POST（大小写不敏感）", required = true) @RequestParam String method,
			@Parameter(description = "模糊关键字，匹配路径或接口说明") @RequestParam(required = false) String keyword,
			@Parameter(description = "接口形态过滤：admin / app / openapi，不传则返回全部形态") @RequestParam(required = false) String apiType) {
		return ApiResult.ok(ApiMappingUtil.searchApis(method, keyword, apiType));
	}

	/**
	 * 返回注册表中形态为 {@code admin} 的 API 元数据（可选再按 method、keyword 缩小范围）。
	 */
	@Operation(summary = "按形态查询 API：管理端(admin)")
	@GetMapping("/apis/type/admin")
	public ApiResult<List<ApiMeta>> listApisAdmin(@RequestParam(required = false) String method,
			@RequestParam(required = false) String keyword) {
		return listApisBySurfaceWire("admin", method, keyword);
	}

	/**
	 * 返回注册表中形态为 {@code app} 的 API 元数据（可选再按 method、keyword 缩小范围）。
	 */
	@Operation(summary = "按形态查询 API：应用端(app)")
	@GetMapping("/apis/type/app")
	public ApiResult<List<ApiMeta>> listApisApp(@RequestParam(required = false) String method,
			@RequestParam(required = false) String keyword) {
		return listApisBySurfaceWire("app", method, keyword);
	}

	/**
	 * 返回注册表中形态为 {@code openapi} 的 API 元数据（可选再按 method、keyword 缩小范围）。
	 */
	@Operation(summary = "按形态查询 API：开放 API(openapi)")
	@GetMapping("/apis/type/openapi")
	public ApiResult<List<ApiMeta>> listApisOpenApi(@RequestParam(required = false) String method,
			@RequestParam(required = false) String keyword) {
		return listApisBySurfaceWire("openapi", method, keyword);
	}

	private static ApiResult<List<ApiMeta>> listApisBySurfaceWire(String surfaceWire, String method, String keyword) {
		return ApiResult.ok(ApiMappingUtil.searchApis(method, keyword, surfaceWire));
	}
}
