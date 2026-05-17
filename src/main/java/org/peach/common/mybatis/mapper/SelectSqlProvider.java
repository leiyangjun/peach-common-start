package org.peach.common.mybatis.mapper;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mvc.exception.BizException;
import org.peach.common.mybatis.code.CrudBizCode;
import org.peach.common.mybatis.model.vo.BigPageVO;
import org.peach.common.mybatis.model.vo.RangeVO;
import org.peach.common.mybatis.model.vo.SearchVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.springframework.util.CollectionUtils;

/**
 * {@link BaseMapper} 查询、唯一键校验与模糊查询等 {@link org.apache.ibatis.annotations.SelectProvider} 入口，
 * 与 {@link SearchVO}、{@link SortVO}、{@link BigPageVO} 等配合生成 {@code SELECT} 与 {@code COUNT} 动态 SQL。
 *
 * @author leiyangjun
 */
public class SelectSqlProvider {
	public String selectBaseSQL(@Param("entity") Object entity, @Param("sort") SortVO sort) {
		String tableName = CommonSqlProvider.getTableName(entity);
		List<String> columns = CommonSqlProvider.getTableColumns(entity, true);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(entity), ",");
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(entity, column) != null) {
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisEntityParam(entity, column));
			}
		}
		String orderBy = CommonSqlProvider.orderBy(entity, sort);
		if (!StringUtils.isEmpty(orderBy)) {
			baseSQL.ORDER_BY(orderBy);
		}
		return baseSQL.toString();
	}

	public String selectBaseAll(@Param("entity") Object entity, @Param("sort") SortVO sort) {
		String tableName = CommonSqlProvider.getTableName(entity);
		List<String> columns = CommonSqlProvider.getTableColumns(entity);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(entity), ",");
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(entity, column) != null) {
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisEntityParam(entity, column));
			}
		}
		String orderBy = CommonSqlProvider.orderBy(entity, sort);
		if (!StringUtils.isEmpty(orderBy)) {
			baseSQL.ORDER_BY(orderBy);
		}
		return baseSQL.toString();
	}

	public String bigPageBaseSQL(@Param("entity") Object entity, @Param("bigPage") BigPageVO bigPage) {
		if (bigPage == null) {
			throw BizException.validWarn(CrudBizCode.CURSOR_PAGE_REQUIRED);
		}
		Integer pageSize = bigPage.getPageSize();
		if (pageSize == null || pageSize < 1) {
			throw BizException.validWarn(CrudBizCode.CURSOR_PAGE_SIZE_INVALID);
		}
		String tableName = CommonSqlProvider.getTableName(entity);
		List<String> columns = CommonSqlProvider.getTableColumns(entity, true);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(entity), ",");
		String tableKey = CommonSqlProvider.getKey(entity, false);
		if (StringUtils.isBlank(tableKey) || !columns.contains(tableKey)) {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(entity, column) != null) {
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisEntityParam(entity, column));
			}
		}
		if (!CommonSqlProvider.isEmptyKeyValue(bigPage.getLastId())) {
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + " > #{bigPage.lastId}");
		}
		baseSQL.ORDER_BY(CommonSqlProvider.sqlColumnName(tableKey) + " asc");
		return baseSQL.toString() + " limit #{bigPage.pageSize}";
	}

	public String selectBaseOneSQL(Object entity) {
		String tableName = CommonSqlProvider.getTableName(entity);
		List<String> columns = CommonSqlProvider.getTableColumns(entity);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(entity), ",");
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(entity, column) != null) {
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisRootParam(entity, column));
			}
		}
		return baseSQL.toString() + " limit 1";
	}

	public <U> String selectBaseByKeysSQL(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass, @Param("sort") SortVO sort) {
		String tableName = CommonSqlProvider.getTableName(voClass);
		// List<String> columns = CommonSqlProvider.getTableColumns(voClass, false);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(voClass), ",");
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		StringBuilder sb = new StringBuilder();
		MessageFormat mf = new MessageFormat("#'{'list[{0,number,#}]}");
		boolean isReturn = true;
		if (!CollectionUtils.isEmpty(list)) {
			for (int i = 0; i < list.size(); i++) {
				sb.append(mf.format(new Object[] { i }));
				// sb.append("'").append(list.get(i)).append("'");
				if (i < list.size() - 1) {
					sb.append(",");
				}
				if (list.get(i) == null || "".equals(list.get(i))) {
					isReturn = false;
				}
			}
		}
		isReturn = CollectionUtils.isEmpty(list) ? false : isReturn;
		if (isReturn && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT(xinhao.toUpperCase());
			baseSQL.FROM(tableName);
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + " IN (" + sb.toString() + ")");
			String orderBy = CommonSqlProvider.orderBy(voClass, sort);
			if (!StringUtils.isEmpty(orderBy)) {
				baseSQL.ORDER_BY(orderBy);
			}
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}

	public String checkBaseSQL(Object obj) {
		String tableName = CommonSqlProvider.getTableName(obj);
		List<String> columns = CommonSqlProvider.getTableColumns(obj);
		String tableKey = CommonSqlProvider.getKey(obj, false);
		if (columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT("COUNT(1)");
			baseSQL.FROM(tableName);
			for (String column : columns) {
				if (!column.equals(tableKey) && CommonSqlProvider.hasNonEmptyValue(obj, column) && !CommonSqlProvider.isCreateAuditColumn(column)) {
					baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisRootParam(obj, column));
				}
			}
			// 初次数据库没有数据时ID为空,也应该支持
			if (CommonSqlProvider.hasNonEmptyValue(obj, tableKey)) {
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + "<>" + CommonSqlProvider.mybatisRootParam(obj, tableKey));
			}
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}

	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkUnique} 的 {@code COUNT(1)}，
	 * 与 {@link #selectUniqueSQL} 同属单列 {@link org.peach.common.mybatis.annotation.Unique} 语义。
	 */
	public String checkUniqueSQL(@Param("uniqueValue") Object uniqueValue, @Param("voClass") Class<?> voClass,
			@Param("excludeKey") Serializable excludeKey) {
		return buildCheckUniqueDuplicateSql(uniqueValue, voClass, excludeKey, false).toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkExist} 的 {@code COUNT(1)>0}。
	 */
	public String checkExistSQL(@Param("uniqueValue") Object uniqueValue, @Param("voClass") Class<?> voClass,
			@Param("excludeKey") Serializable excludeKey) {
		return buildCheckUniqueDuplicateSql(uniqueValue, voClass, excludeKey, true).toString();
	}
	private SQL buildCheckUniqueDuplicateSql(Object uniqueValue, Class<?> voClass, Serializable excludeKey,
			boolean countAsExistHint) {
		List<String> uniques = CommonSqlProvider.getUnique(voClass);
		if (CollectionUtils.isEmpty(uniques)) {
			throw BizException.validWarn(CrudBizCode.UNIQUE_FIELD_REQUIRED_ON_VO);
		}
		if (uniques.size() > 1) {
			throw BizException.validWarn(CrudBizCode.UNIQUE_FIELD_MULTIPLE_ON_VO);
		}
		if (uniqueValue == null) {
			throw BizException.validWarn(CrudBizCode.UNIQUE_QUERY_VALUE_REQUIRED);
		}
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		if (!columns.contains(tableKey)) {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
		String tableName = CommonSqlProvider.getTableName(voClass);
		String uniqueField = uniques.get(0);
		SQL baseSQL = new SQL();
		baseSQL.SELECT(countAsExistHint ? "COUNT(1)>0" : "COUNT(1)");
		baseSQL.FROM(tableName);
		baseSQL.WHERE(CommonSqlProvider.sqlColumnName(uniqueField) + "=#{uniqueValue}");
		if (!CommonSqlProvider.isEmptyKeyValue(excludeKey)) {
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + "<>#{excludeKey}");
		}
		return baseSQL;
	}

	public String selectUniqueSQL(@Param("uniqueValue") Object uniqueValue, @Param("voClass") Class<?> voClass) {
		return buildSelectUniqueCoreSql(uniqueValue, voClass).toString() + " limit 1";
	}

	public String selectUniqueValidSQL(@Param("uniqueValue") Object uniqueValue, @Param("voClass") Class<?> voClass) {
		SQL baseSQL = buildSelectUniqueCoreSql(uniqueValue, voClass);
		String logicField = CommonSqlProvider.getLogicDeleteField(voClass, false);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		if (StringUtils.isNotEmpty(logicField) && columns.contains(logicField)) {
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(logicField) + "=" + CommonSqlProvider.getLogicValidValue(voClass));
		}
		return baseSQL.toString() + " limit 1";
	}

	private SQL buildSelectUniqueCoreSql(Object uniqueValue, Class<?> voClass) {
		List<String> uniques = CommonSqlProvider.getUnique(voClass);
		if (CollectionUtils.isEmpty(uniques)) {
			throw BizException.validWarn(CrudBizCode.UNIQUE_FIELD_REQUIRED_ON_VO);
		}
		if (uniques.size() > 1) {
			throw BizException.validWarn(CrudBizCode.UNIQUE_FIELD_MULTIPLE_ON_VO);
		}
		if (uniqueValue == null) {
			throw BizException.validWarn(CrudBizCode.UNIQUE_QUERY_VALUE_REQUIRED);
		}
		String tableName = CommonSqlProvider.getTableName(voClass);
		String selectList = StringUtils.join(CommonSqlProvider.getUpperTableColumns(voClass), ",").toUpperCase();
		SQL baseSQL = new SQL();
		baseSQL.SELECT(selectList);
		baseSQL.FROM(tableName);
		String uniqueField = uniques.get(0);
		baseSQL.WHERE(CommonSqlProvider.sqlColumnName(uniqueField) + "=#{uniqueValue}");
		return baseSQL;
	}

	public String likeSelectBaseSQL(@Param("entity") Object entity, @Param("search") SearchVO search,
			@Param("range") RangeVO range, @Param("sort") SortVO sort) {
		String tableName = CommonSqlProvider.getTableName(entity);
		List<String> columns = CommonSqlProvider.getTableColumns(entity, true);
		// columns.add("searchValue");
		List<String> searchvalues = CommonSqlProvider.getSearchValue(entity);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(entity), ",");
		StringBuilder sb = new StringBuilder();
		if (!CollectionUtils.isEmpty(searchvalues)) {
			for (int i = 0; i < searchvalues.size(); i++) {
				// sb.append(CommonSqlProvider.sqlColumnName(searchvalues.get(i)) + " like
				// concat('%',concat(#{searchValue},'%')) ");
				sb.append(CommonSqlProvider.sqlColumnName(searchvalues.get(i)) + " like concat('%', #{search.searchValue}, '%') ");
				if (i < searchvalues.size() - 1) {
					sb.append("or  ");
				}
			}
		}
		SQL baseSQL = new SQL();

		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(entity, column) != null && !CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(entity, column))) {
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisEntityParam(entity, column));
			}
		}
		String searchValStr = search == null ? null : search.getSearchValue();
		if (!StringUtils.isEmpty(searchValStr) && !CollectionUtils.isEmpty(searchvalues)) {
			baseSQL.WHERE("(" + sb.toString() + ")");
		}
		if (range != null) {
			CommonSqlProvider.appendRangeConditions(baseSQL, entity, range);
		}
		String orderBy = CommonSqlProvider.orderBy(entity, sort);
		if (!StringUtils.isEmpty(orderBy)) {
			baseSQL.ORDER_BY(orderBy);
		}
		return baseSQL.toString();
	}

	public String selectBaseByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = CommonSqlProvider.getTableName(voClass);
		// List<String> columns = CommonSqlProvider.getTableColumns(voClass, false);
		String xinhao = StringUtils.join(CommonSqlProvider.getUpperTableColumns(voClass), ",");
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		if (key != null && !"".equals(key) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT(xinhao.toUpperCase());
			baseSQL.FROM(tableName);
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}
}
