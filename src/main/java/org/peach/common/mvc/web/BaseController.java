package org.peach.common.mvc.web;

import java.io.Serializable;
import java.util.Objects;

import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mybatis.model.vo.PageVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.mybatis.service.BaseAbstractService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.github.pagehelper.PageInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;

/**
 * 通用 REST 基类：入参/出参均为 VO，依赖服务层提供查询、保存与分页能力（不含物理删除）。
 * <p>
 * 分页为 {@code GET /page}，筛选条件与 VO 类型 {@code V} 的属性同名，通过查询参数绑定（{@link ModelAttribute}）；子类需标注
 * {@code @RestController} 与 {@code @RequestMapping("...")}。
 * </p>
 *
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/users")
 * public class UserController extends BaseController&lt;UserVO, UserService&gt; {
 * 	public UserController(UserService service) {
 * 		super(service);
 * 	}
 * }
 * </pre>
 *
 * <p>
 * 对应 Service 实现类可继承
 * {@link org.peach.common.mybatis.service.BaseAbstractService} 复用默认能力。
 * </p>
 *
 * @param <V> 对外 VO 类型
 * @param <S> Service 类型
 * @author leiyangjun
 */
public abstract class BaseController<V extends Serializable, S extends BaseAbstractService<?, ?, V>> {

	protected final S service;

	protected BaseController(S service) {
		this.service = Objects.requireNonNull(service, "service");
	}

	@Operation(summary = "根据主键查询详情", description = "按路径参数主键查询单条记录；无记录时由统一响应封装与异常处理决定返回内容。")
	@GetMapping("/{id}")
	public ApiResult<V> getById(
			@Parameter(name = "id", description = "主键值", required = true, in = ParameterIn.PATH) @PathVariable String id) {
		return ApiResult.ok(service.getById(id));
	}

	@Operation(summary = "保存或更新", description = "新增时不能传入主键；更新时须传入有效主键。更新路径下通常仅非 null 字段参与 SET。请求体为 VO。")
	@PostMapping
	public ApiResult<V> save(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "VO JSON：新增请勿带主键；更新须带有效主键", required = true) @Valid @RequestBody V body) {
		return ApiResult.ok(service.save(body));
	}

	@Operation(summary = "精确条件分页查询（GET）", description = "分页与排序使用 pageNum、pageSize、sortName、sortType 查询参数；与 VO 类型 V 同名的查询参数作为等值筛选条件（未传或为空的属性不参与 WHERE）。与按主键查询 GET .../{id} 在路径模式上不冲突；仅当主键取值可能为字符串 page 时，才可能与分页路径 /page 混淆，需在业务上规避。")
	@GetMapping("/page")
	public ApiResult<PageInfo<V>> page(@ModelAttribute V condition, PageVO page, SortVO sort) {
		return ApiResult.ok(service.listPage(condition, page, sort));
	}
}
