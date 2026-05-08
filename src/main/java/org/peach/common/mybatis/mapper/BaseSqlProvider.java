package org.peach.common.mybatis.mapper;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mybatis.annotation.Exclude;
import org.peach.common.mybatis.annotation.ID;
import org.peach.common.mybatis.annotation.LogicDelete;
import org.peach.common.mybatis.annotation.Range;
import org.peach.common.mybatis.annotation.SearchValue;
import org.peach.common.mybatis.annotation.TableName;
import org.peach.common.mybatis.annotation.Unique;
import org.peach.common.mybatis.exception.PersistenceException;
import org.peach.common.mybatis.model.vo.BigPageVO;
import org.peach.common.mybatis.model.vo.CommonQueryVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.mybatis.support.AuditBridge;
import org.peach.common.mybatis.support.BeanProperties;
import org.peach.common.utils.IdUtil;
import org.springframework.util.CollectionUtils;

/**
 * 为 {@link org.peach.common.mybatis.mapper.BaseMapper} 各方法提供动态
 * SQL（{@code Provider}）， 依据实体上的
 * {@link org.peach.common.mybatis.annotation.TableName}、{@link org.peach.common.mybatis.annotation.ID}、
 * {@link org.peach.common.mybatis.annotation.LogicDelete}、{@link org.peach.common.mybatis.annotation.Range}
 * 等注解拼装语句。
 * <p>
 * 区间条件由 {@link org.peach.common.mybatis.annotation.Range} 与入参中非 null
 * 字段共同决定；具体语义见各方法说明。
 * </p>
 *
 * @author leiyangjun
 * @since 0.0.1-SNAPSHOT
 */
public class BaseSqlProvider {

	private static final Pattern SAFE_FIELD_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");
	private static final Set<String> CREATE_AUDIT_COLUMNS = Set.of("creator", "createTime", "creatorId", "creatorName");
	private static final Set<String> EXCLUDED_BASE_COLUMNS = Set.of("creator", "editor", "createTime", "editTime",
			"creatorId", "editorId", "creatorName", "editorName");

	/**
	 * Provider 内部错误文案（用于抛出 {@link PersistenceException}，统一由全局异常处理打印日志并返回消息体）。
	 */
	private static final class ProviderErrors {
		private ProviderErrors() {
		}

		private static String tableKeyError() {
			return "主键缺失或无效，请检查 @ID 与入参";
		}

		private static String logicKeyError() {
			return "逻辑删除字段或主键配置无效";
		}

		private static String dbFieldError() {
			return "表字段注解配置冲突（如同类上多个 @ID / @LogicDelete）";
		}

		/** selectUnique / selectUniqueValid：条件对象中没有任何 @Unique 字段为非 null 时 */
		private static String selectUniqueNoCondition() {
			return "按唯一键查询时，条件对象中至少应有一个 @Unique 字段为非 null";
		}

		/** voClass 上未标注任何 @Unique 时 */
		private static String selectUniqueNoAnnotatedFields() {
			return "按唯一键查询时，voClass 须包含至少一个 @Unique 字段";
		}

		/** voClass 上标注多个 @Unique，与单字段唯一查询语义冲突 */
		private static String selectUniqueMultiAnnotatedFields() {
			return "按唯一键查询时，仅允许一个 @Unique 字段";
		}
	}

	/** 读取实体属性（实例），避免 JSON 往返序列化。 */
	private static Object prop(Object bean, String name) {
		return BeanProperties.read(bean, name);
	}

	/** 主键/字符串类条件是否已填写：非 null 且非空串。 */
	private static boolean hasNonEmptyValue(Object bean, String name) {
		return !BeanProperties.isEmptyKey(prop(bean, name));
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private static boolean isCreateAuditColumn(String column) {
		return CREATE_AUDIT_COLUMNS.contains(column);
	}

	private static boolean containsIgnoreCaseText(String source, String part) {
		return lower(source).contains(lower(part));
	}

	private static String findColumnIgnoreCase(List<String> columns, String target) {
		String expected = lower(target);
		for (String column : columns) {
			if (expected.equals(lower(column))) {
				return column;
			}
		}
		return null;
	}

	private static void appendEditorAuditSet(SQL sql, List<String> columns) {
		String editTimeColumn = findColumnIgnoreCase(columns, "editTime");
		if (editTimeColumn != null) {
			sql.SET(rename(editTimeColumn) + "=CURRENT_TIMESTAMP");
		}
		String editorIdColumn = findColumnIgnoreCase(columns, "editorId");
		String editorColumn = findColumnIgnoreCase(columns, "editor");
		String editorNameColumn = findColumnIgnoreCase(columns, "editorName");
		if (editorIdColumn != null && StringUtils.isNotBlank(AuditBridge.getUserId())) {
			sql.SET(rename(editorIdColumn) + "='" + AuditBridge.getUserId() + "'");
		}
		if (editorColumn != null && StringUtils.isNotBlank(AuditBridge.getUserId())) {
			sql.SET(rename(editorColumn) + "='" + AuditBridge.getUserId() + "'");
		}
		if (editorNameColumn != null && StringUtils.isNotBlank(AuditBridge.getUserName())) {
			sql.SET(rename(editorNameColumn) + "='" + AuditBridge.getUserName() + "'");
		}
	}

	/**
	 * 追加区间条件：实体中仅允许一个字段标记 {@link Range}，并使用 query 的 startValue/endValue。
	 */
	private static void appendRangeConditions(SQL sql, Object entity, CommonQueryVO query) {
		if (query == null) {
			return;
		}
		List<Field> rangeFields = FieldUtils.getAllFieldsList(entity.getClass()).stream()
				.filter(field -> field.getAnnotation(Range.class) != null).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(rangeFields)) {
			return;
		}
		if (rangeFields.size() > 1) {
			throw new PersistenceException("仅允许一个 @Range 字段，当前数量=" + rangeFields.size());
		}
		String column = rename(rangeFields.get(0).getName());
		if (query.getStartValue() != null) {
			sql.WHERE(column + " >= #{query.startValue}");
		}
		if (query.getEndValue() != null) {
			sql.WHERE(column + " <= #{query.endValue}");
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#insertBase} 的 INSERT：值为
	 * null 的列不写；主键与逻辑删除未赋值时按注解补默认。
	 *
	 * @param obj 实体实例
	 * @return 动态 SQL 字符串
	 */
	public String insertBaseSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		String tableKey = getKey(obj, false);
		String logicField = getLogicDeleteField(obj, false);
		String tableKeyDefaultValue = getTableKeyValue(obj, false);
		SQL baseSQL = new SQL();
		baseSQL.INSERT_INTO(tableName);
		for (String column : columns) {
			if (prop(obj, column) != null && !column.equals(tableKey) && !column.equals(logicField)) {// 如果字段为null,不计入此处操作
				baseSQL.VALUES(rename(column), "#{" + column + "}");
			}
			// VALUES(rename(tableKey),"REPLACE(UUID(),''-'','''')");
			if (column.equals(tableKey) && hasNonEmptyValue(obj, tableKey)) {
				baseSQL.VALUES(rename(tableKey), "#{" + tableKey + "}");
			} else if (column.equals(tableKey) && !StringUtils.isEmpty(tableKeyDefaultValue)) {
				baseSQL.VALUES(rename(tableKey), tableKeyDefaultValue);
			}
			if (column.equals(logicField)) {
				baseSQL.VALUES(rename(logicField),
						BeanProperties.isEmptyKey(prop(obj, logicField)) ? getLogicValidValue(obj)
								: "#{" + column + "}");
			}
		}
		return baseSQL.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#insertBaseAll} 的
	 * INSERT：含 null 列；主键与逻辑删除策略同 {@link #insertBaseSQL}。
	 *
	 * @param obj 实体实例
	 * @return 动态 SQL 字符串
	 */
	public String insertBaseAllSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		String tableKey = getKey(obj, false);
		String logicField = getLogicDeleteField(obj, false);
		String tableKeyDefaultValue = getTableKeyValue(obj, false);
		SQL baseSQL = new SQL();
		baseSQL.INSERT_INTO(tableName);
		for (String column : columns) {
			if (!column.equals(tableKey) && !column.equals(logicField)) {// 如果字段为null,不计入此处操作
				baseSQL.VALUES(rename(column), "#{" + column + "}");
			}
			if (column.equals(tableKey) && hasNonEmptyValue(obj, tableKey)) {
				baseSQL.VALUES(rename(tableKey), "#{" + tableKey + "}");
			} else if (column.equals(tableKey) && !StringUtils.isEmpty(tableKeyDefaultValue)) {
				baseSQL.VALUES(rename(tableKey), tableKeyDefaultValue);
			}
			if (column.equals(logicField)) {
				baseSQL.VALUES(rename(logicField),
						BeanProperties.isEmptyKey(prop(obj, logicField)) ? getLogicValidValue(obj)
								: "#{" + column + "}");
			}
		}
		return baseSQL.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#deleteBase} 的物理 DELETE：非
	 * null 字段等值匹配（空串亦匹配），可能删多条。
	 *
	 * @param obj 条件实体
	 * @return 动态 SQL 字符串
	 */
	public String deleteBaseSQL(Object obj) {
		// 将className以驼峰规则加入下划线
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		SQL baseSQL = new SQL();
		baseSQL.DELETE_FROM(tableName);
		for (String column : columns) {
			if (prop(obj, column) != null) {// 如果字段为null,不计入此处操作
				baseSQL.WHERE(rename(column) + "=#{" + column + "}");
			}
		}
		return baseSQL.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#updateBase} 的
	 * UPDATE：主键必填；仅 SET 非 null 非审计创建列。
	 *
	 * @param obj 含主键的实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键或主键为空时
	 */
	public String updateBaseSQL(Object obj) {
		// 将className以驼峰规则加入下划线
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey) && hasNonEmptyValue(obj, tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			for (String column : columns) {
				// getCreateColumns
				// 如果字段为null,切创建人字段不包含更新&& !jsonObject.get(column).equals("")
				if (prop(obj, column) != null && !isCreateAuditColumn(column)) {
					baseSQL.SET(rename(column) + "=#{" + column + "}");
				}
			}
			baseSQL.WHERE(rename(tableKey) + "=#{" + tableKey + "}");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#updateBaseAll} 的
	 * UPDATE：主键必填；非主键列含 null 也会写入。
	 *
	 * @param obj 含主键的实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键或主键为空时
	 */
	public String updateBaseAllSQL(Object obj) {
		// 将className以驼峰规则加入下划线
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey) && hasNonEmptyValue(obj, tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			for (String column : columns) {
				// getCreateColumns
				// 如果字段为null,且创建人字段不包含更新
				if (!column.equals(tableKey) && !isCreateAuditColumn(column)) {
					baseSQL.SET(rename(column) + "=#{" + column + "}");
				}
			}
			baseSQL.WHERE(rename(tableKey) + "=#{" + tableKey + "}");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectBase} 的
	 * SELECT：实体等值条件 + 可选 {@link SortVO}（无模糊、无 {@link CommonQueryVO} 区间）。
	 *
	 * @param entity 查询实体（等值条件）
	 * @param sort   排序（可为 {@code null}）
	 * @return 动态 SQL 字符串
	 */
	public String selectBaseSQL(@Param("entity") Object entity, @Param("sort") SortVO sort) {
		String tableName = getTableName(entity);
		List<String> columns = getTableColumns(entity, true);
		String xinhao = StringUtils.join(getUpperTableColumns(entity), ",");
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (prop(entity, column) != null) {
				baseSQL.WHERE(rename(column) + "=#{entity." + column + "}");
			}
		}
		String orderBy = orderBy(entity, sort);
		if (!StringUtils.isEmpty(orderBy)) {
			baseSQL.ORDER_BY(orderBy);
		}
		return baseSQL.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectBaseAll} 的
	 * SELECT：列集合与 {@link #selectBaseSQL} 略有不同；无模糊与 CommonQueryVO 区间。
	 *
	 * @param entity 查询实体（等值条件）
	 * @param sort   排序（可为 {@code null}）
	 * @return 动态 SQL 字符串
	 */
	public String selectBaseAll(@Param("entity") Object entity, @Param("sort") SortVO sort) {
		String tableName = getTableName(entity);
		List<String> columns = getTableColumns(entity);
		String xinhao = StringUtils.join(getUpperTableColumns(entity), ",");
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (prop(entity, column) != null) {
				baseSQL.WHERE(rename(column) + "=#{entity." + column + "}");
			}
		}
		String orderBy = orderBy(entity, sort);
		if (!StringUtils.isEmpty(orderBy)) {
			baseSQL.ORDER_BY(orderBy);
		}
		return baseSQL.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#bigPageBase} 的大数据量游标分页 SQL：
	 * 固定主键升序 + {@code LIMIT pageSize}；当 {@code lastId} 非空时追加 {@code 主键 > lastId}。
	 *
	 * @param entity  查询实体（非 null 字段等值）
	 * @param bigPage 游标分页参数
	 * @return 动态 SQL 字符串
	 */
	public String bigPageBaseSQL(@Param("entity") Object entity, @Param("bigPage") BigPageVO bigPage) {
		if (bigPage == null) {
			throw new PersistenceException("bigPage 不能为空");
		}
		Integer pageSize = bigPage.getPageSize();
		if (pageSize == null || pageSize < 1) {
			throw new PersistenceException("bigPage.pageSize 不能为空且必须大于0");
		}
		String tableName = getTableName(entity);
		List<String> columns = getTableColumns(entity, true);
		String xinhao = StringUtils.join(getUpperTableColumns(entity), ",");
		String tableKey = getKey(entity, false);
		if (StringUtils.isBlank(tableKey) || !columns.contains(tableKey)) {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (prop(entity, column) != null) {
				baseSQL.WHERE(rename(column) + "=#{entity." + column + "}");
			}
		}
		if (!BeanProperties.isEmptyKey(bigPage.getLastId())) {
			baseSQL.WHERE(rename(tableKey) + " > #{bigPage.lastId}");
		}
		baseSQL.ORDER_BY(rename(tableKey) + " asc");
		return baseSQL.toString() + " limit #{bigPage.pageSize}";
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectBaseOne} 的
	 * SELECT：条件同全列策略列表查询，仅追加 {@code LIMIT 1}，不包含排序。
	 * <p>
	 * 入参须为 Mapper 单参数形态（无 {@code @Param}），占位符使用 {@code #{column}}，与 MyBatis 单参数绑定规则一致；
	 * 避免 Provider 收到 {@code ParamMap} 时在 Java 17+ 触发 {@code InaccessibleObjectException}。
	 * </p>
	 *
	 * @param entity 查询实体（等值条件）
	 * @return 动态 SQL 字符串
	 */
	public String selectBaseOneSQL(Object entity) {
		String tableName = getTableName(entity);
		List<String> columns = getTableColumns(entity);
		String xinhao = StringUtils.join(getUpperTableColumns(entity), ",");
		SQL baseSQL = new SQL();
		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (prop(entity, column) != null) {
				baseSQL.WHERE(rename(column) + "=#{" + column + "}");
			}
		}
		return baseSQL.toString() + " limit 1";
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#batchInsertBase} 的批量
	 * INSERT IGNORE；主键策略按首元素推断；逻辑删除列用默认值字面量。
	 *
	 * @param list 同结构实体列表，非空
	 * @return 动态 SQL 字符串
	 */
	public <T> String batchInsertBaseSQL(List<T> list) {
		T t = list.get(0);
		String tableName = getTableName(t);
		String tableKey = getKey(t, false);
		String logicKey = getLogicDeleteField(t, false);
		// String tabkeKeyDefaultValue=getTableKeyValue(t);
		List<String> columns = getTableColumns(t);// 判断是否过滤掉主键字段
		// 当T中不带主键属性,且为自动增长MYSQL时,不需要将ID生成SQL片段
		columns = columns.stream().filter(column -> {
			boolean result = true;
			List<Field> filedTemp = getFilterAnnotation(t, ID.class);
			Field tableKeyField = !CollectionUtils.isEmpty(filedTemp) ? filedTemp.get(0) : null;
			tableKeyField = tableKeyField == null ? getDeclaredField(t, tableKey) : tableKeyField;
			ID tableKeyAn = tableKeyField == null ? null : tableKeyField.getAnnotation(ID.class);
			if (BeanProperties.isEmptyKey(prop(t, tableKey)) && column.equals(tableKey) && tableKeyAn != null
					&& tableKeyAn.isSequence() && StringUtils.isEmpty(tableKeyAn.sequenceTag())) {
				result = false;
			}
			return result;
		}).collect(Collectors.toList());

		String xinhao = StringUtils.join(columns.stream().map(column -> rename(column)).collect(Collectors.toList()),
				",");
		StringBuilder sb = new StringBuilder();
		StringBuilder temp = new StringBuilder();
		temp.append("(");
		for (int j = 0; j < columns.size(); j++) {
			if (BeanProperties.isEmptyKey(prop(t, logicKey)) && columns.contains(logicKey)
					&& columns.get(j).equals(logicKey)) {
				temp.append("\'\'" + getLogicValidValue(t) + "\'\'");
			} else {
				temp.append("#'{'list[{0,number,#}]." + columns.get(j) + "}");
			}
			if (j < columns.size() - 1) {
				temp.append(",");
			}
		}
		temp.append(")");
		// String str = temp.toString();
		sb.append("INSERT IGNORE " + tableName + " (" + xinhao + ") VALUES ");
		for (int i = 0; i < list.size(); i++) {
			// String str = "";

			MessageFormat mf = new MessageFormat("");
			if (BeanProperties.isEmptyKey(prop(t, tableKey)) && columns.contains(tableKey)) {
				// str = StringUtils.replace(str, "#'{'list[{0}]." + tableKey + "}",
				// "REPLACE(UUID(),''-'','''')");
				mf.applyPattern(
						temp.toString().replace("#'{'list[{0,number,#}]." + tableKey + "}", getTableKeyValue(t, true)));
			} else {
				mf.applyPattern(temp.toString());
			}
			// MessageFormat mf = new MessageFormat(str);
			sb.append(mf.format(new Object[] { i }));
			if (i < list.size() - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#batchDeleteBase} 的 IN
	 * 删除：从每条 VO 取主键；任一空主键则抛错。更推荐 {@link #batchDeleteBaseByKeysSQL}。
	 *
	 * @param list 含主键的实体列表
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 主键缺失或无效时
	 */
	public <T> String batchDeleteBaseSQL(List<T> list) {
		T t = list.get(0);
		String tableName = getTableName(t);
		String tableKey = getKey(t, false);
		List<String> columns = getTableColumns(t);
		StringBuilder sb = new StringBuilder();
		MessageFormat mf = new MessageFormat("#'{'list[{0,number,#}]." + tableKey + "}");
		boolean isReturn = true;
		if (!CollectionUtils.isEmpty(list)) {
			for (int i = 0; i < list.size(); i++) {
				sb.append(mf.format(new Object[] { i }));
				if (i < list.size() - 1) {
					sb.append(",");
				}
				if (BeanProperties.isEmptyKey(prop(list.get(i), tableKey))) {
					isReturn = false;
				}
			}
		}
		isReturn = CollectionUtils.isEmpty(list) ? false : isReturn;
		if (isReturn && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.DELETE_FROM(tableName);
			baseSQL.WHERE(rename(tableKey) + " IN (" + sb.toString() + ")");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#batchDeleteBaseByKeys}
	 * 的物理 DELETE … IN (主键列表)。
	 *
	 * @param list    主键列表
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 主键列无法解析或列表无效时
	 */
	public <U> String batchDeleteBaseByKeysSQL(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass) {
		String tableName = getTableName(voClass);
		String tableKey = getKey(voClass, false);
		List<String> columns = getTableColumns(voClass);
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
			baseSQL.DELETE_FROM(tableName);
			baseSQL.WHERE(rename(tableKey) + " IN (" + sb.toString() + ")");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectBaseByKeys} 的
	 * SELECT … WHERE 主键 IN (…)。
	 *
	 * @param list    主键列表
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 主键无效时
	 */
	public <U> String selectBaseByKeysSQL(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass, @Param("sort") SortVO sort) {
		String tableName = getTableName(voClass);
		// List<String> columns = getTableColumns(voClass, false);
		String xinhao = StringUtils.join(getUpperTableColumns(voClass), ",");
		String tableKey = getKey(voClass, false);
		List<String> columns = getTableColumns(voClass);
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
			baseSQL.WHERE(rename(tableKey) + " IN (" + sb.toString() + ")");
			String orderBy = orderBy(voClass, sort);
			if (!StringUtils.isEmpty(orderBy)) {
				baseSQL.ORDER_BY(orderBy);
			}
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkBase} 的
	 * {@code COUNT(1)}：非主键非空字段等值；有主键则排除本行。实体须含主键列定义。
	 *
	 * @param obj 待校验实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键映射时
	 */
	public String checkBaseSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT("COUNT(1)");
			baseSQL.FROM(tableName);
			for (String column : columns) {
				if (!column.equals(tableKey) && hasNonEmptyValue(obj, column) && !isCreateAuditColumn(column)) {
					baseSQL.WHERE(rename(column) + "=#{" + column + "}");
				}
			}
			// 初次数据库没有数据时ID为空,也应该支持
			if (hasNonEmptyValue(obj, tableKey)) {
				baseSQL.WHERE(rename(tableKey) + "<>#{" + tableKey + "}");
			}
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}

	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkUnique} 的
	 * {@code COUNT(1)}：仅 {@link Unique} 标注列参与条件，主键用于排除当前行。
	 *
	 * @param obj 待校验实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键映射时
	 */
	public String checkUniqueSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		List<String> uniques = getUnique(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT("COUNT(1)");
			baseSQL.FROM(tableName);
			for (String column : columns) {
				if (uniques.contains(column)) {
					baseSQL.WHERE(rename(column) + "=#{" + column + "}");
				}
			}
			// 初次数据库没有数据时ID为空,也应该支持
			if (hasNonEmptyValue(obj, tableKey)) {
				baseSQL.WHERE(rename(tableKey) + "<>#{" + tableKey + "}");
			}
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkExist} 的
	 * {@code COUNT(1)>0}，条件语义同 {@link #checkUniqueSQL}。
	 *
	 * @param obj 待校验实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键映射时
	 */
	public String checkExistSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		List<String> uniques = getUnique(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT("COUNT(1)>0");
			baseSQL.FROM(tableName);
			for (String column : columns) {
				if (uniques.contains(column)) {
					baseSQL.WHERE(rename(column) + "=#{" + column + "}");
				}
			}
			// 初次数据库没有数据时ID为空,也应该支持
			if (hasNonEmptyValue(obj, tableKey)) {
				baseSQL.WHERE(rename(tableKey) + "<>#{" + tableKey + "}");
			}
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectUnique}：元数据来自
	 * {@code voClass}，条件值来自 {@code entity}；仅 {@link Unique} 列且属性非
	 * {@code null} 参与等值 WHERE，<strong>不使用主键</strong>（适合「只带用户名查 admin」等场景），
	 * 列集合同 {@link #selectBaseOneSQL}，末尾 {@code LIMIT 1}。
	 *
	 * @param entity  条件对象（如仅设置 userName=admin）
	 * @param voClass 表映射类型（解析表名、列、@Unique）
	 * @return 动态 SQL
	 */
	public String selectUniqueSQL(@Param("entity") Object entity, @Param("voClass") Class<?> voClass) {
		return buildSelectUniqueCoreSql(entity, voClass).toString() + " limit 1";
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectUniqueValid}：在
	 * {@link #selectUniqueSQL} 条件上追加逻辑删除列为有效值（无映射列则不加该项）。
	 *
	 * @param entity  条件对象
	 * @param voClass 表映射类型
	 * @return 动态 SQL
	 */
	public String selectUniqueValidSQL(@Param("entity") Object entity, @Param("voClass") Class<?> voClass) {
		SQL baseSQL = buildSelectUniqueCoreSql(entity, voClass);
		String logicField = getLogicDeleteField(voClass, false);
		List<String> columns = getTableColumns(voClass);
		if (StringUtils.isNotEmpty(logicField) && columns.contains(logicField)) {
			baseSQL.WHERE(rename(logicField) + "=" + getLogicValidValue(voClass));
		}
		return baseSQL.toString() + " limit 1";
	}

	/**
	 * {@link #selectUniqueSQL} / {@link #selectUniqueValidSQL} 共用的 SELECT + FROM + WHERE（不含 limit）。
	 */
	private SQL buildSelectUniqueCoreSql(Object entity, Class<?> voClass) {
		List<String> uniques = getUnique(voClass);
		if (CollectionUtils.isEmpty(uniques)) {
			throw new PersistenceException(ProviderErrors.selectUniqueNoAnnotatedFields());
		}
		if (uniques.size() > 1) {
			throw new PersistenceException(ProviderErrors.selectUniqueMultiAnnotatedFields());
		}
		String tableName = getTableName(voClass);
		String selectList = StringUtils.join(getUpperTableColumns(voClass), ",").toUpperCase();
		SQL baseSQL = new SQL();
		baseSQL.SELECT(selectList);
		baseSQL.FROM(tableName);
		String uniqueField = uniques.get(0);
		Object uniqueValue = prop(entity, uniqueField);
		if (uniqueValue == null) {
			throw new PersistenceException(ProviderErrors.selectUniqueNoCondition());
		}
		baseSQL.WHERE(rename(uniqueField) + "=#{entity." + uniqueField + "}");
		return baseSQL;
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkMultOrUnique} 的
	 * {@code COUNT(1)}：各 {@link Unique} 列用 OR 组合，主键排除当前行。
	 *
	 * @param obj 待校验实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键映射时
	 */
	public String checkMultOrUniqueSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		List<String> uniques = getUnique(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT("COUNT(1)");
			baseSQL.FROM(tableName);
			StringBuilder multSB = new StringBuilder();
			for (String column : columns) {
				if (uniques.contains(column)) {
					multSB.append("OR " + rename(column) + "=#{" + column + "} ");
				}
			}
			baseSQL.WHERE("(" + multSB.toString().replaceFirst("OR", "") + ")");
			// 初次数据库没有数据时ID为空,也应该支持
			if (hasNonEmptyValue(obj, tableKey)) {
				baseSQL.WHERE(rename(tableKey) + "<>#{" + tableKey + "}");
			}
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#checkMultOrExist} 的
	 * {@code COUNT(1)>0}，OR 组合语义同 {@link #checkMultOrUniqueSQL}。
	 *
	 * @param obj 待校验实体
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 无主键映射时
	 */
	public String checkMultOrExistSQL(Object obj) {
		String tableName = getTableName(obj);
		List<String> columns = getTableColumns(obj);
		List<String> uniques = getUnique(obj);
		String tableKey = getKey(obj, false);
		if (columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT("COUNT(1)>0");
			baseSQL.FROM(tableName);
			StringBuilder multSB = new StringBuilder();
			for (String column : columns) {
				if (uniques.contains(column)) {
					multSB.append("OR " + rename(column) + "=#{" + column + "} ");
				}
			}
			baseSQL.WHERE("(" + multSB.toString().replaceFirst("OR", "") + ")");
			// 初次数据库没有数据时ID为空,也应该支持
			if (hasNonEmptyValue(obj, tableKey)) {
				baseSQL.WHERE(rename(tableKey) + "<>#{" + tableKey + "}");
			}
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#likeSelectBase} 的
	 * SELECT：{@link SearchValue} 列对 {@code searchValue} 做 LIKE；其余非空列等值；含
	 * {@link Range}（来自 query）与 {@link SortVO} 排序。
	 *
	 * @param entity 查询实体（等值条件）
	 * @param query  模糊搜索/区间（不含排序字段）
	 * @param sort   排序（可为 {@code null}）
	 * @return 动态 SQL 字符串
	 */
	public String likeSelectBaseSQL(@Param("entity") Object entity, @Param("query") CommonQueryVO query,
			@Param("sort") SortVO sort) {
		String tableName = getTableName(entity);
		List<String> columns = getTableColumns(entity, true);
		// columns.add("searchValue");
		List<String> searchvalues = getSearchValue(entity);
		String xinhao = StringUtils.join(getUpperTableColumns(entity), ",");
		StringBuilder sb = new StringBuilder();
		if (!CollectionUtils.isEmpty(searchvalues)) {
			for (int i = 0; i < searchvalues.size(); i++) {
				// sb.append(rename(searchvalues.get(i)) + " like
				// concat('%',concat(#{searchValue},'%')) ");
				sb.append(rename(searchvalues.get(i)) + " like concat('%', #{query.searchValue}, '%') ");
				if (i < searchvalues.size() - 1) {
					sb.append("or  ");
				}
			}
		}
		SQL baseSQL = new SQL();

		baseSQL.SELECT(xinhao.toUpperCase());
		baseSQL.FROM(tableName);
		for (String column : columns) {
			if (prop(entity, column) != null && !BeanProperties.isEmptyKey(prop(entity, column))) {
				baseSQL.WHERE(rename(column) + "=#{entity." + column + "}");
			}
		}
		String searchValStr = query == null ? null : query.getSearchValue();
		if (!StringUtils.isEmpty(searchValStr) && !CollectionUtils.isEmpty(searchvalues)) {
			baseSQL.WHERE("(" + sb.toString() + ")");
		}
		appendRangeConditions(baseSQL, entity, query);
		String orderBy = orderBy(entity, sort);
		if (!StringUtils.isEmpty(orderBy)) {
			baseSQL.ORDER_BY(orderBy);
		}
		return baseSQL.toString();
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#selectBaseByKey} 按主键查单条。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 主键无效时
	 */
	public String selectBaseByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = getTableName(voClass);
		// List<String> columns = getTableColumns(voClass, false);
		String xinhao = StringUtils.join(getUpperTableColumns(voClass), ",");
		String tableKey = getKey(voClass, false);
		List<String> columns = getTableColumns(voClass);
		if (key != null && !"".equals(key) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.SELECT(xinhao.toUpperCase());
			baseSQL.FROM(tableName);
			baseSQL.WHERE(rename(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#deleteBaseByKey}
	 * 按主键物理删一条。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 主键无效时
	 */
	public String deleteBaseByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = getTableName(voClass);
		String tableKey = getKey(voClass, false);
		List<String> columns = getTableColumns(voClass);
		if (key != null && !"".equals(key) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.DELETE_FROM(tableName);
			baseSQL.WHERE(rename(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.tableKeyError());
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#logicDeleteByKey}：将
	 * {@link LogicDelete} 列置为无效值，并可顺带写审计字段。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 缺少逻辑删除列或主键时
	 */
	public String logicDeleteByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = getTableName(voClass);
		List<String> columns = getTableColumns(voClass);
		String logicDeleteField = getLogicDeleteField(voClass, false);
		String tableKey = getKey(voClass, false);
		if (key != null && !"".equals(key) && columns.contains(logicDeleteField) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();

			baseSQL.UPDATE(tableName);
			baseSQL.SET(rename(logicDeleteField) + "=" + getLogicInvalidValue(voClass));
			appendEditorAuditSet(baseSQL, columns);
			baseSQL.WHERE(rename(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.logicKeyError());
		}
	}

	/**
	 * 生成
	 * {@link org.peach.common.mybatis.mapper.BaseMapper#logicRecoveryByKey}：将逻辑删除列恢复为有效值。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 缺少逻辑删除列或主键时
	 */
	public String logicRecoveryByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = getTableName(voClass);
		List<String> columns = getTableColumns(voClass);
		String logicDeleteField = getLogicDeleteField(voClass, false);
		String tableKey = getKey(voClass, false);
		if (key != null && !"".equals(key) && columns.contains(logicDeleteField) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			baseSQL.SET(rename(logicDeleteField) + "=" + getLogicValidValue(voClass));
			appendEditorAuditSet(baseSQL, columns);
			baseSQL.WHERE(rename(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.logicKeyError());
		}
	}

	/**
	 * 生成
	 * {@link org.peach.common.mybatis.mapper.BaseMapper#logicBatchDeleteKeys}：按主键
	 * IN 批量逻辑删除。
	 *
	 * @param list    主键列表
	 * @param voClass 实体类型
	 * @return 动态 SQL 字符串
	 * @throws PersistenceException 列表或列配置无效时
	 */
	public <U> String logicBatchDeleteKeysSQL(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass) {
		String tableName = getTableName(voClass);
		String tableKey = getKey(voClass, false);
		List<String> columns = getTableColumns(voClass);
		String logicDeleteField = getLogicDeleteField(voClass, false);
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
		if (isReturn && columns.contains(logicDeleteField) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			baseSQL.SET(rename(logicDeleteField) + "=" + getLogicInvalidValue(voClass));
			appendEditorAuditSet(baseSQL, columns);
			baseSQL.WHERE(rename(tableKey) + " IN (" + sb.toString() + ")");
			return baseSQL.toString();
		} else {
			throw new PersistenceException(ProviderErrors.logicKeyError());
		}
	}

	/**
	 * 解析逻辑表名：优先 {@link TableName#value()}，否则由类简单名驼峰转下划线大写（如 {@code User} →
	 * {@code USER}）。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 表名
	 */
	public static String getTableName(Object entityClass) {
		Class<?> clazz = entityClass instanceof Class ? (Class<?>) entityClass : entityClass.getClass();
		TableName tn = clazz.getAnnotation(TableName.class);
		if (tn != null && StringUtils.isNotBlank(tn.value())) {
			return tn.value();
		}
		return rename(clazz.getSimpleName());
	}

	/**
	 * 返回参与 SQL 的实体属性名列表（驼峰小写）；排除 {@link Exclude}、{@link Range} 等非表列字段，见
	 * {@link #getFilterAnnotationName}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 属性名列表
	 */
	public static List<String> getTableColumns(Object entityClass) {
		return getFilterAnnotationName(entityClass, null);
	}

	/**
	 * 在 {@link #getTableColumns(Object)} 基础上，若 {@code containBase} 为 {@code false}
	 * 则移除常见审计字段名（creator/editor/createtime 等）。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param containBase 为 {@code true} 时保留审计列
	 * @return 属性名列表
	 */
	public static List<String> getTableColumns(Object entityClass, boolean containBase) {
		List<String> columns = getTableColumns(entityClass);
		if (!containBase) {
			columns.removeIf(EXCLUDED_BASE_COLUMNS::contains);
		}
		return columns;
	}

	/**
	 * 将 {@link #getTableColumns(Object)} 得到的每个属性名经 {@link #rename} 转为库侧大写下划线形式，供
	 * SELECT 列清单使用。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 列名列表（大写下划线）
	 */
	public static List<String> getUpperTableColumns(Object entityClass) {
		return getFilterAnnotationName(entityClass, null).stream().map(field -> rename(field))
				.collect(Collectors.toList());
	}

	/**
	 * 解析单主键属性名：优先 {@link ID} 标注字段，否则默认 {@code id}；不支持复合主键，多处标注将抛错。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param rename      为 {@code true} 时返回 {@link #rename} 后的列名
	 * @return 主键字段名，无效时可能为空串
	 * @throws PersistenceException 存在多个 {@link ID} 时
	 */
	public static String getKey(Object entityClass, boolean rename) {
		String tableKes = "id";
		List<String> fields = getFilterAnnotationName(entityClass, ID.class);
		if (!CollectionUtils.isEmpty(fields) && fields.size() > 1) {
			throw new PersistenceException(ProviderErrors.dbFieldError());
		}
		tableKes = !CollectionUtils.isEmpty(fields) ? fields.get(0) : tableKes;
		tableKes = checkFieldExists(tableKes, entityClass) ? tableKes : "";
		return rename ? rename(tableKes) : tableKes;
	}

	/**
	 * 解析逻辑删除属性名：优先 {@link LogicDelete} 字段，否则默认 {@code valid}；规则同 {@link #getKey} 的
	 * rename 与存在性校验。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param rename      为 {@code true} 时返回库列名形式
	 * @return 属性名或空串
	 * @throws PersistenceException 多处 {@link LogicDelete} 时
	 */
	public static String getLogicDeleteField(Object entityClass, boolean rename) {
		String tableKes = "valid";
		List<String> fields = getFilterAnnotationName(entityClass, LogicDelete.class);
		if (!CollectionUtils.isEmpty(fields) && fields.size() > 1) {
			throw new PersistenceException(ProviderErrors.dbFieldError());
		}
		tableKes = !CollectionUtils.isEmpty(fields) ? fields.get(0) : tableKes;
		tableKes = checkFieldExists(tableKes, entityClass) ? tableKes : "";
		return rename ? rename(tableKes) : tableKes;
	}

	/**
	 * 判断某属性是否视为「表映射字段」：存在、非静态 final、非 {@link Exclude}，且类型为 JDK 基本包装等。
	 *
	 * @param field       属性名
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 是否可作为表列参与 SQL
	 */
	public static boolean checkFieldExists(String field, Object entityClass) {
		Field fieldTemp = getDeclaredField(entityClass, field);
		boolean result = true;
		if (fieldTemp == null || fieldTemp.getType().getClassLoader() != null
				|| !fieldTemp.getType().getName().startsWith("java") || Modifier.isStatic(fieldTemp.getModifiers())
				|| Modifier.isFinal(fieldTemp.getModifiers()) || fieldTemp.getAnnotation(Exclude.class) != null) {
			result = false;
		} /**
			 * else if () { result = false; } else if
			 * (!fieldTemp.getType().getName().startsWith("java")) { result = false; } else
			 * if (Modifier.isStatic(fieldTemp.getModifiers()) ||
			 * Modifier.isFinal(fieldTemp.getModifiers())) { result = false; } else if
			 * (fieldTemp.getAnnotation(Exclude.class) != null) { result = false; }
			 */
		return result;
	}

	/**
	 * 返回标注了 {@link LogicDelete} 的 {@link Field}，唯一；否则无注解时返回 {@code null}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 字段或 {@code null}
	 * @throws PersistenceException 多处 {@link LogicDelete} 时
	 */
	public static Field getLogicDeleteField(Object entityClass) {
		List<Field> fields = getFilterAnnotation(entityClass, LogicDelete.class);
		if (!CollectionUtils.isEmpty(fields) && fields.size() > 1) {
			throw new PersistenceException(ProviderErrors.dbFieldError());
		}
		return !CollectionUtils.isEmpty(fields) ? fields.get(0) : null;
	}

	/**
	 * 主键缺省时的 SQL 片段：{@link String} 类用
	 * {@link IdUtil#shortId22()}；{@code long}/{@link Long} 用
	 * {@link IdUtil#nextId()}； 自增/序列见
	 * {@link ID#isSequence()}、{@link ID#sequenceTag()}；显式
	 * {@link ID#isSnowflakeHash()} 强制雪花。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param msgFormat   为批量插入 MessageFormat 转义预留的引号形式（仅影响字符串主键）
	 * @return 可直接拼进 SQL 的字面量或表达式字符串
	 */
	public static String getTableKeyValue(Object entityClass, boolean msgFormat) {
		String tableKeyStr = getKey(entityClass, false);
		Field tableKeyField = getDeclaredField(entityClass, tableKeyStr);
		ID idAn = tableKeyField == null ? null : tableKeyField.getAnnotation(ID.class);
		Class<?> ft = tableKeyField != null ? tableKeyField.getType() : String.class;

		if (idAn != null && idAn.isSequence() && StringUtils.isEmpty(idAn.sequenceTag())) {
			return "";
		}
		if (idAn != null && idAn.isSequence() && StringUtils.isNotEmpty(idAn.sequenceTag())) {
			return idAn.sequenceTag();
		}
		if (idAn != null && idAn.isSnowflakeHash()) {
			return formatKeyNumberSql(IdUtil.nextId());
		}
		if (isIntegralNumericKeyType(ft)) {
			return formatKeyNumberSql(IdUtil.nextId());
		}
		if (isStringLikeKeyType(ft)) {
			return formatKeyStringSql(IdUtil.shortId22(), msgFormat);
		}
		// 其它类型（如无 @ID）：与 String 相同采用短 ID，避免再依赖 32 位 UUID
		return formatKeyStringSql(IdUtil.shortId22(), msgFormat);
	}

	/** 整型数值主键（含 {@code long}/{@link Long}，及 int/Integer 等），空值时统一走雪花。 */
	private static boolean isIntegralNumericKeyType(Class<?> t) {
		if (t == null) {
			return false;
		}
		return t == Long.class || t == long.class || t == Integer.class || t == int.class || t == Short.class
				|| t == short.class || t == Byte.class || t == byte.class;
	}

	private static boolean isStringLikeKeyType(Class<?> t) {
		return t != null && CharSequence.class.isAssignableFrom(t);
	}

	/** 数值主键 SQL 片段（不加引号）。 */
	private static String formatKeyNumberSql(long id) {
		return String.valueOf(id);
	}

	/** 字符串主键 SQL 字面量。 */
	private static String formatKeyStringSql(String id, boolean msgFormat) {
		return msgFormat ? "\'\'\'" + id + "\'\'\'" : "\'" + id + "\'";
	}

	/**
	 * 在全部声明字段中筛选：若指定 {@code filterAnnotation} 则只保留带该注解的；否则保留「可作为表列」的 Java 字段（含对
	 * {@link Range} 等的排除规则）。
	 *
	 * @param entityClass      实体实例或 {@link Class}
	 * @param filterAnnotation 限定注解类型，可为 {@code null} 表示不按单注解过滤
	 * @param <T>              注解类型
	 * @return 字段列表
	 */
	public static <T extends Annotation> List<Field> getFilterAnnotation(Object entityClass,
			Class<T> filterAnnotation) {
		List<Field> fieldList = Arrays.stream(getDeclaredFields(entityClass)).filter(field -> {// 过滤,只需要逻辑删除字段
			boolean result = true;
			if (filterAnnotation != null && field.getAnnotation(filterAnnotation) != null) {
				result = true;
			} else if (field.getType().getClassLoader() != null || !field.getType().getName().startsWith("java")
					|| Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
					|| field.getAnnotation(Exclude.class) != null || field.getAnnotation(Range.class) != null
					|| filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) {
				result = false;
			}
			/**
			 * else if (!field.getType().getName().startsWith("java")) { result = false; }
			 * else if (Modifier.isStatic(field.getModifiers()) ||
			 * Modifier.isFinal(field.getModifiers())) { result = false; } else if
			 * (field.getAnnotation(Exclude.class) != null) { result = false; } else if
			 * (filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) {
			 * result = false; }
			 */
			return result;
		}).collect(Collectors.toList());
		return fieldList;
	}

	/**
	 * 逻辑删除「有效」侧取值：字段名含 valid/delete 时按惯例推断，否则取 {@link LogicDelete#valid()}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 有效状态字面量
	 */
	public static String getLogicValidValue(Object entityClass) {
		String field = BaseSqlProvider.getLogicDeleteField(entityClass, false);
		String isDelete = containsIgnoreCaseText(field, "valid") ? "1" : "0";
		isDelete = containsIgnoreCaseText(field, "delete") ? "0" : isDelete;
		Field logicField = getDeclaredField(entityClass, field);
		if (!containsIgnoreCaseText(field, "valid") && !containsIgnoreCaseText(field, "delete") && logicField != null) {
			LogicDelete logicAn = logicField.getAnnotation(LogicDelete.class);
			isDelete = logicAn != null ? String.valueOf(logicAn.valid()) : isDelete;
		}
		return isDelete;
	}

	/**
	 * 逻辑删除「无效」侧取值：规则与 {@link #getLogicValidValue} 对称，否则取
	 * {@link LogicDelete#invalid()}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 无效状态字面量
	 */
	public static String getLogicInvalidValue(Object entityClass) {
		String field = BaseSqlProvider.getLogicDeleteField(entityClass, false);
		String isDelete = containsIgnoreCaseText(field, "valid") ? "0" : "1";
		isDelete = containsIgnoreCaseText(field, "delete") ? "1" : isDelete;
		Field logicField = getDeclaredField(entityClass, field);
		if (!containsIgnoreCaseText(field, "valid") && !containsIgnoreCaseText(field, "delete") && logicField != null) {
			LogicDelete logicAn = logicField.getAnnotation(LogicDelete.class);
			isDelete = logicAn != null ? String.valueOf(logicAn.invalid()) : isDelete;
		}
		return isDelete;
	}

	/**
	 * {@link #getFilterAnnotation} 的属性名列表形式。
	 *
	 * @param entityClass      实体实例或 {@link Class}
	 * @param filterAnnotation 限定注解，可为 {@code null}
	 * @param <T>              注解类型
	 * @return 属性名列表
	 */
	public static <T extends Annotation> List<String> getFilterAnnotationName(Object entityClass,
			Class<T> filterAnnotation) {
		List<String> fieldList = Arrays.stream(getDeclaredFields(entityClass)).filter(field -> {// 过滤,只需要逻辑删除字段
			boolean result = true;
			if (filterAnnotation != null && field.getAnnotation(filterAnnotation) != null) {
				result = true;
			} else if (field.getType().getClassLoader() != null || !field.getType().getName().startsWith("java")
					|| Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
					|| field.getAnnotation(Exclude.class) != null || field.getAnnotation(Range.class) != null
					|| filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) {
				result = false;
			}
			return result;
		}).map(Field::getName).collect(Collectors.toList());
		return fieldList;
	}

	/**
	 * 返回标有 {@link SearchValue} 的属性名，供模糊条件使用；库列名需再经 {@link #rename}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 属性名列表
	 */
	public static List<String> getSearchValue(Object entityClass) {
		return getFilterAnnotationName(entityClass, SearchValue.class);
	}

	/**
	 * 实体类及其父类上的全部 {@link Field}（含私有，与 MyBatis 反射习惯一致）。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 字段数组
	 */
	public static Field[] getDeclaredFields(Object entityClass) {
		Class<?> clazz = entityClass instanceof Class ? (Class<?>) entityClass : entityClass.getClass();
		List<Field> all = FieldUtils.getAllFieldsList(clazz);
		return all.toArray(new Field[0]);
	}

	/**
	 * 按名查找字段（向上继承链查找），不存在返回 {@code null}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param name        属性名
	 * @return 字段或 {@code null}
	 */
	public static Field getDeclaredField(Object entityClass, String name) {
		Class<?> clazz = entityClass instanceof Class ? (Class<?>) entityClass : entityClass.getClass();
		return FieldUtils.getField(clazz, name, true);
	}

	/**
	 * 返回标有 {@link Unique} 的属性名列表。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 属性名列表
	 */
	public static List<String> getUnique(Object entityClass) {
		return getFilterAnnotationName(entityClass, Unique.class);
	}

	/**
	 * 驼峰属性名 → 大写下划线列名（如 {@code abcEfg} → {@code ABC_EFG}）。
	 *
	 * @param name 属性名
	 * @return 列名
	 */
	public static String rename(String name) {
		return name.replaceAll("[A-Z]", " $0").trim().replaceAll(" ", "_").toUpperCase();
	}

	/**
	 * 下划线大写列名转驼峰属性名。
	 *
	 * @param name       如 {@code ABC_EFF}
	 * @param firstLower 为 {@code true} 时首字母小写（默认）
	 * @return 驼峰名
	 */
	public static String underlineToCamel(String name, Boolean firstLower) {
		if (StringUtils.isBlank(name)) {
			return name;
		}
		boolean firstLowerTemp = firstLower == null || firstLower;
		String normalized = StringUtils.lowerCase(name);
		StringBuilder sb = new StringBuilder(normalized.length());
		boolean upperNext = false;
		for (int i = 0; i < normalized.length(); i++) {
			char ch = normalized.charAt(i);
			if (ch == '_') {
				upperNext = true;
				continue;
			}
			if (upperNext) {
				sb.append(Character.toUpperCase(ch));
				upperNext = false;
			} else {
				sb.append(ch);
			}
		}
		if (!firstLowerTemp && sb.length() > 0) {
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		}
		return sb.toString();
	}

	/**
	 * {@link #underlineToCamel(String, Boolean)} 的首字母小写简写形式。
	 *
	 * @param name 列名
	 * @return 驼峰名
	 */
	public static String underlineToCamel(String name) {
		return underlineToCamel(name, true);
	}

	/**
	 * 与 {@link #rename} 等价，命名上与 {@link #underlineToCamel} 成对使用。
	 *
	 * @param name 属性名
	 * @return 大写下划线列名
	 */
	public static String camelToUnderLine(String name) {
		return name.replaceAll("[A-Z]", " $0").trim().replaceAll(" ", "_").toUpperCase();
	}

	/**
	 * 从查询对象上读取 {@code sortName}/{@code sortType} 生成 {@code ORDER BY} 片段；未指定时尝试
	 * {@code rowno} 升序或 {@code EditTime} 降序。
	 *
	 * @param entityClass 含排序字段的 DTO/实体
	 * @return 排序子句，可能为空串
	 */
	public static String orderBy(Object entityClass) {
		// descending ascending
		return orderBy(entityClass, "");
	}

	/**
	 * 从 {@link SortVO} 读取排序；与 {@link #selectBaseSQL} 列策略一致时使用 {@code getTableColumns(entity, true)}。
	 */
	public static String orderBy(Object entityClass, SortVO sort) {
		if (sort == null) {
			return orderBy(entityClass, "");
		}
		String sortName = sort.getSortName();
		String sortType = sort.getSortType();
		if (StringUtils.isNotBlank(sortName) && StringUtils.isNotBlank(sortType)) {
			return orderByFromExplicitSort(entityClass, StringUtils.trim(sortName), StringUtils.trim(sortType));
		}
		return orderBy(entityClass, "");
	}

	private static String orderByFromExplicitSort(Object entityClass, String sortName, String sortType) {
		if (!SAFE_FIELD_NAME.matcher(sortName).matches()) {
			throw new PersistenceException("排序字段非法：" + sortName);
		}
		List<String> columns = getTableColumns(entityClass, true);
		if (!columns.contains(sortName)) {
			throw new PersistenceException("排序字段不存在：" + sortName);
		}
		sortType = lower(sortType);
		sortType = (sortType.startsWith("asc") || "0".equals(sortType)) ? "asc" : sortType;
		sortType = (sortType.startsWith("desc") || "1".equals(sortType)) ? "desc" : sortType;
		if (!"asc".equals(sortType) && !"desc".equals(sortType)) {
			throw new PersistenceException("排序方式非法：" + sortType);
		}
		return rename(sortName) + " " + sortType;
	}

	/**
	 * 同 {@link #orderBy(Object)}，可为排序列加表别名前缀（联表场景）。
	 *
	 * @param entityClass 含排序字段的 DTO/实体
	 * @param aliasTable  表别名，可为空串
	 * @return 排序子句，可能为空串
	 */
	public static String orderBy(Object entityClass, String aliasTable) {
		// descending ascending
		String orderBy = "";
		List<String> columns = getTableColumns(entityClass);
		Object sortNameObj = BeanProperties.read(entityClass, "sortName");
		Object sortTypeObj = BeanProperties.read(entityClass, "sortType");
		String sortName = sortNameObj == null ? null : String.valueOf(sortNameObj);
		String sortType = sortTypeObj == null ? null : String.valueOf(sortTypeObj);
		if (!StringUtils.isEmpty(sortName) && !StringUtils.isEmpty(sortType)) {
			sortType = lower(sortType);
			sortType = sortType.startsWith("asc") ? "asc" : sortType;
			sortType = sortType.startsWith("desc") ? "desc" : sortType;
			sortType = "0".equals(sortType) ? "asc" : sortType;
			sortType = "1".equals(sortType) ? "desc" : sortType;
			orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + rename(sortName) + " " + sortType;
		} else {// 默认按照修改时间或者行号排序
			// columns.rowno
			for (String column : columns) {// 如果没有传,有行号,按行号排序,没有行号按时间排序
				if ("rowno".equalsIgnoreCase(column)) {
					orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + rename(column) + " asc";
					break;
				}
			}
			if (StringUtils.isEmpty(orderBy)) {
				for (String column : columns) {
					if ("EditTime".equalsIgnoreCase(column)) {
						orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + rename(column) + " desc";
						break;
					}
				}
			}
		}
		return orderBy;
	}

}
