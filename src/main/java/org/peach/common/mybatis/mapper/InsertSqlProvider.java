package org.peach.common.mybatis.mapper;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mybatis.annotation.ID;
import org.peach.common.utils.LoginUserUtil;
import org.springframework.util.CollectionUtils;

/**
 * {@link BaseMapper} 插入与批量插入对应的 MyBatis {@link org.apache.ibatis.annotations.InsertProvider} 实现：
 * 按实体注解生成 {@code INSERT} / {@code INSERT IGNORE} 动态 SQL，主键与逻辑删除默认值由 {@link CommonSqlProvider} 解析。
 *
 * @author leiyangjun
 */
public class InsertSqlProvider {

	public String insertBaseSQL(Object obj) {
		CommonSqlProvider.ensureApplicationGeneratedPrimaryKey(obj);
		String tableName = CommonSqlProvider.getTableName(obj);
		List<String> columns = CommonSqlProvider.getTableColumns(obj);
		String tableKey = CommonSqlProvider.getKey(obj, false);
		String logicField = CommonSqlProvider.getLogicDeleteField(obj, false);
		String tableKeyDefaultValue = CommonSqlProvider.getTableKeyValue(obj, false);
		Long auditUid = LoginUserUtil.getLoginUserId();
		SQL baseSQL = new SQL();
		baseSQL.INSERT_INTO(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(obj, column) != null && !column.equals(tableKey) && !column.equals(logicField)) {// 如果字段为null,不计入此处操作
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(column), CommonSqlProvider.mybatisRootParam(obj, column));
			} else if (auditUid != null && CommonSqlProvider.isInsertAuditColumnName(column) && !column.equals(tableKey)
					&& !column.equals(logicField)) {
				String lit = CommonSqlProvider.insertAuditValueLiteral(obj, column, auditUid);
				if (lit != null) {
					baseSQL.VALUES(CommonSqlProvider.sqlColumnName(column), lit);
				}
			}
			// VALUES(CommonSqlProvider.sqlColumnName(tableKey),"REPLACE(UUID(),''-'','''')");
			if (column.equals(tableKey) && CommonSqlProvider.hasNonEmptyValue(obj, tableKey)) {
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(tableKey), CommonSqlProvider.mybatisRootParam(obj, tableKey));
			} else if (column.equals(tableKey) && !StringUtils.isEmpty(tableKeyDefaultValue)) {
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(tableKey), tableKeyDefaultValue);
			}
			if (column.equals(logicField)) {
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(logicField),
						CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(obj, logicField))
								? CommonSqlProvider.getLogicValidValue(obj)
								: CommonSqlProvider.mybatisRootParam(obj, column));
			}
		}
		return baseSQL.toString();
	}

	public String insertBaseAllSQL(Object obj) {
		CommonSqlProvider.ensureApplicationGeneratedPrimaryKey(obj);
		String tableName = CommonSqlProvider.getTableName(obj);
		List<String> columns = CommonSqlProvider.getTableColumns(obj);
		String tableKey = CommonSqlProvider.getKey(obj, false);
		String logicField = CommonSqlProvider.getLogicDeleteField(obj, false);
		String tableKeyDefaultValue = CommonSqlProvider.getTableKeyValue(obj, false);
		Long auditUid = LoginUserUtil.getLoginUserId();
		SQL baseSQL = new SQL();
		baseSQL.INSERT_INTO(tableName);
		for (String column : columns) {
			if (!column.equals(tableKey) && !column.equals(logicField)) {// 如果字段为null,不计入此处操作
				if (auditUid != null && CommonSqlProvider.prop(obj, column) == null
						&& CommonSqlProvider.isInsertAuditColumnName(column)) {
					String lit = CommonSqlProvider.insertAuditValueLiteral(obj, column, auditUid);
					if (lit != null) {
						baseSQL.VALUES(CommonSqlProvider.sqlColumnName(column), lit);
						continue;
					}
				}
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(column), CommonSqlProvider.mybatisRootParam(obj, column));
			}
			if (column.equals(tableKey) && CommonSqlProvider.hasNonEmptyValue(obj, tableKey)) {
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(tableKey), CommonSqlProvider.mybatisRootParam(obj, tableKey));
			} else if (column.equals(tableKey) && !StringUtils.isEmpty(tableKeyDefaultValue)) {
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(tableKey), tableKeyDefaultValue);
			}
			if (column.equals(logicField)) {
				baseSQL.VALUES(CommonSqlProvider.sqlColumnName(logicField),
						CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(obj, logicField))
								? CommonSqlProvider.getLogicValidValue(obj)
								: CommonSqlProvider.mybatisRootParam(obj, column));
			}
		}
		return baseSQL.toString();
	}

	public <T> String batchInsertBaseSQL(List<T> list) {
		for (T row : list) {
			CommonSqlProvider.ensureApplicationGeneratedPrimaryKey(row);
		}
		T t = list.get(0);
		String tableName = CommonSqlProvider.getTableName(t);
		String tableKey = CommonSqlProvider.getKey(t, false);
		String logicKey = CommonSqlProvider.getLogicDeleteField(t, false);
		// String tabkeKeyDefaultValue=CommonSqlProvider.getTableKeyValue(t);
		List<String> columns = CommonSqlProvider.getTableColumns(t);// 判断是否过滤掉主键字段
		// 当T中不带主键属性,且为自动增长MYSQL时,不需要将ID生成SQL片段
		columns = columns.stream().filter(column -> {
			boolean result = true;
			List<Field> filedTemp = CommonSqlProvider.getFilterAnnotation(t, ID.class);
			Field tableKeyField = !CollectionUtils.isEmpty(filedTemp) ? filedTemp.get(0) : null;
			tableKeyField = tableKeyField == null ? CommonSqlProvider.getDeclaredField(t, tableKey) : tableKeyField;
			ID tableKeyAn = tableKeyField == null ? null : tableKeyField.getAnnotation(ID.class);
			if (CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(t, tableKey)) && column.equals(tableKey)
					&& tableKeyAn != null && tableKeyAn.isSequence() && StringUtils.isEmpty(tableKeyAn.sequenceTag())) {
				result = false;
			}
			return result;
		}).collect(Collectors.toList());

		String xinhao = StringUtils.join(
				columns.stream().map(column -> CommonSqlProvider.sqlColumnName(column)).collect(Collectors.toList()), ",");
		StringBuilder sb = new StringBuilder();
		StringBuilder temp = new StringBuilder();
		temp.append("(");
		for (int j = 0; j < columns.size(); j++) {
			if (CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(t, logicKey)) && columns.contains(logicKey)
					&& columns.get(j).equals(logicKey)) {
				temp.append("\'\'" + CommonSqlProvider.getLogicValidValue(t) + "\'\'");
			} else {
				temp.append(CommonSqlProvider.batchInsertListPropertySlot(t.getClass(), columns.get(j)));
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
			if (CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(t, tableKey)) && columns.contains(tableKey)) {
				// str = StringUtils.replace(str, "#'{'list[{0}]." + tableKey + "}",
				// "REPLACE(UUID(),''-'','''')");
				mf.applyPattern(temp.toString().replace(
						CommonSqlProvider.batchInsertListPropertySlot(t.getClass(), tableKey),
						CommonSqlProvider.getTableKeyValue(t, true)));
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

}
