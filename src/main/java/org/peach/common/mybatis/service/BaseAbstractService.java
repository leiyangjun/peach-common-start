package org.peach.common.mybatis.service;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.peach.common.mybatis.mapper.BaseMapper;
import org.peach.common.mybatis.mapper.CommonSqlProvider;
import org.peach.common.mybatis.model.vo.SearchVO;
import org.peach.common.mybatis.model.vo.PageVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.utils.BeanUtil;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * 单表通用 Service（方案 A）：对外使用 VO，对内与 {@link BaseMapper} 使用 Entity；提供查询、保存与分页，不提供物理删除。
 * <p>
 * VO ↔ Entity 统一由 {@link BeanUtil#copy(Object, Class)} 按同名属性浅拷贝；须具备无参构造。若映射规则复杂，可继承本类后
 * 重写 {@link #getById}、{@link #listPage}、{@link #save}、{@link #toVoPageInfo} 等；默认分页走
 * {@link org.peach.common.mybatis.mapper.BaseMapper#likeSelectBase}（等值 + 可选关键字，区间参数传 {@code null}），或不用本基类、在业务 Service 中手写转换。
 * </p>
 *
 * @param <M> Mapper 类型
 * @param <E> 持久化实体（与表映射）
 * @param <V> 对外 VO（Controller 入参/出参）
 * @author leiyangjun
 */
public abstract class BaseAbstractService<M extends BaseMapper<E>, E extends Serializable, V extends Serializable>
		implements BaseInterfaceService<V> {

	protected final M mapper;
	protected final Class<E> entityClass;
	protected final Class<V> voClass;

	protected BaseAbstractService(M mapper, Class<E> entityClass, Class<V> voClass) {
		this.mapper = Objects.requireNonNull(mapper, "mapper");
		this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
		this.voClass = Objects.requireNonNull(voClass, "voClass");
	}

	@Transactional(readOnly = true)
	public V getById(Serializable id) {
		E row = mapper.selectBaseByKey(id, entityClass);
		return row == null ? null : BeanUtil.copy(row, voClass);
	}

	/**
	 * 默认分页：等值条件来自 VO 拷贝实体；关键字传入 Mapper 模糊片段；调用 Mapper 时区间固定传 {@code null}（不拼区间 SQL）。
	 * <p>
	 * 对外仍不暴露 {@link org.peach.common.mybatis.model.vo.RangeVO}；若列表需要区间过滤，请子类重写本方法或自定义 Mapper。
	 * </p>
	 */
	@Override
	@Transactional(readOnly = true)
	public PageInfo<V> listPage(V condition, SearchVO searchVO, PageVO page, SortVO sort) {
		PageHelper.startPage(page.getPageNum(), page.getPageSize());
		E cond = condition == null ? null : BeanUtil.copy(condition, entityClass);
		List<E> rows = mapper.likeSelectBase(cond, searchVO, null, sort);
		return toVoPageInfo(new PageInfo<>(rows));
	}

	@Transactional
	public Serializable save(V vo) {
		E entity = BeanUtil.copy(vo, entityClass);
		if (!isPrimaryKeyPresent(entity)) {
			mapper.insertBase(entity);
		} else {
			mapper.updateBase(entity);
		}
		Object pk = readPrimaryKeyValue(entity);
		if (pk != null && !(pk instanceof Serializable)) {
			throw new IllegalStateException("主键类型须实现 java.io.Serializable，当前类型=" + pk.getClass().getName());
		}
		return (Serializable) pk;
	}

	/**
	 * 将 PageHelper 分页结果转为 VO 分页；保留 total、pages 等元数据。需定制列表元素映射时可重写。
	 */
	protected PageInfo<V> toVoPageInfo(PageInfo<E> src) {
		if (src == null) {
			return null;
		}
		PageInfo<V> dest = new PageInfo<>();
		BeanUtil.copyProperties(src, dest, "list");
		List<E> raw = src.getList();
		if (raw == null) {
			dest.setList(Collections.emptyList());
		} else {
			dest.setList(raw.stream().map(e -> BeanUtil.copy(e, voClass)).collect(Collectors.toList()));
		}
		return dest;
	}

	private boolean isPrimaryKeyPresent(E entity) {
		Object v = readPrimaryKeyValue(entity);
		if (v == null) {
			return false;
		}
		if (v instanceof CharSequence) {
			return StringUtils.isNotBlank((CharSequence) v);
		}
		return true;
	}

	private Object readPrimaryKeyValue(E entity) {
		String pkProp = CommonSqlProvider.getKey(entity, false);
		if (StringUtils.isBlank(pkProp)) {
			return null;
		}
		Field f = CommonSqlProvider.getDeclaredField(entity, pkProp);
		if (f == null) {
			return null;
		}
		try {
			return FieldUtils.readField(f, entity, true);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("读取主键属性失败: " + pkProp, e);
		}
	}
}
