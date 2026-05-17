package org.peach.common.mvc.web;

import java.io.Serializable;
import java.util.Objects;

import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mybatis.model.vo.SearchVO;
import org.peach.common.mybatis.model.vo.PageVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.mybatis.service.BaseInterfaceService;
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
 * 分页为 {@code GET /page}：等值筛选与 VO 类型 {@code V} 的属性同名，通过 {@link ModelAttribute} 绑定；关键字参数
 * {@code searchValue} 绑定至独立的 {@link SearchVO}。子类需标注 {@code @RestController} 与
 * {@code @RequestMapping("...")}。
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
 * 泛型 {@code S} 为业务 Service 接口（继承 {@link BaseInterfaceService}）；实现类可继承
 * {@link org.peach.common.mybatis.service.BaseAbstractService} 复用默认能力，由 Spring 注入接口类型。
 * </p>
 * <p>
 * {@link #save} 的响应 {@code data} 为持久化后的<b>主键</b>（{@link Serializable}，常见为 {@link Long}），不再返回整份 VO。
 * </p>
 * <p>
 * 按主键详情 {@code GET .../{id}} 使用 {@link Long}，与库表 {@code bigint} 及 MyBatis 绑定一致。
 * </p>
 *
 * @param <V> 对外 VO 类型
 * @param <S> Service 类型
 * @author leiyangjun
 */
public abstract class BaseController<V extends Serializable, S extends BaseInterfaceService<V>> {

	protected final S service;

	protected BaseController(S service) {
		this.service = Objects.requireNonNull(service, "service");
	}

	@Operation(summary = "根据主键查询详情", description = "按路径参数主键查询单条记录；无记录时由统一响应封装与异常处理决定返回内容。")
	@GetMapping("/{id}")
	public ApiResult<V> getById(
			@Parameter(name = "id", description = "主键值（数值型，与表 bigint 一致）", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
		return ApiResult.ok(service.getById(id));
	}

	@Operation(summary = "保存或更新", description = "新增时不能传入主键；更新时须传入有效主键。更新路径下通常仅非 null 字段参与 SET。响应 data 为保存后的主键。")
	@PostMapping
	public ApiResult<Serializable> save(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "VO JSON：新增请勿带主键；更新须带有效主键", required = true) @Valid @RequestBody V body) {
		return ApiResult.ok(service.save(body));
	}

	/**
	 * 条件分页（GET）：等值 + 可选关键字；不包含通用区间入参，区间需求由业务层扩展。
	 */
	@Operation(summary = "条件分页查询（GET）", description = "分页与排序：pageNum、pageSize、sortName、sortType；与 VO 类型 V 同名的参数作为等值条件；searchValue 为关键字模糊。与 GET .../{id} 路径不冲突。")
	@GetMapping("/page")
	public ApiResult<PageInfo<V>> page(@ModelAttribute V condition, @ModelAttribute SearchVO searchVO, PageVO page,
			SortVO sort) {
		return ApiResult.ok(service.listPage(condition, searchVO, page, sort));
	}
}
