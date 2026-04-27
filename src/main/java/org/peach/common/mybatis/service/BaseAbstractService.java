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
import org.peach.common.mybatis.mapper.BaseSqlProvider;
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
 * 重写 {@link #getById}、{@link #listPage}、{@link #save}、{@link #toVoPageInfo} 等，或不用本基类、在业务 Service 中手写转换。
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

	@Transactional(readOnly = true)
	public PageInfo<V> listPage(V condition, PageVO page, SortVO sort) {
		PageHelper.startPage(page.getPageNum(), page.getPageSize());
		E cond = condition == null ? null : BeanUtil.copy(condition, entityClass);
		List<E> rows = mapper.selectBase(cond, sort);
		return toVoPageInfo(new PageInfo<>(rows));
	}

	@Transactional
	public V save(V vo) {
		E entity = BeanUtil.copy(vo, entityClass);
		if (!isPrimaryKeyPresent(entity)) {
			mapper.insertBase(entity);
		} else {
			mapper.updateBase(entity);
		}
		return BeanUtil.copy(entity, voClass);
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
		String pkProp = BaseSqlProvider.getKey(entity, false);
		if (StringUtils.isBlank(pkProp)) {
			return null;
		}
		Field f = BaseSqlProvider.getDeclaredField(entity, pkProp);
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
