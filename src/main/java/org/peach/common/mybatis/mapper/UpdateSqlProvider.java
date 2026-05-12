package org.peach.common.mybatis.mapper;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mvc.exception.BizException;
import org.peach.common.mybatis.code.CrudBizCode;
import org.peach.common.utils.LoginUserUtil;
import org.springframework.util.CollectionUtils;

/**
 * {@link BaseMapper} 更新与逻辑删除对应的 {@link org.apache.ibatis.annotations.UpdateProvider}：主键更新、批量逻辑删除与恢复等。
 *
 * @author leiyangjun
 */
public class UpdateSqlProvider {
	
	public String updateBaseSQL(Object obj) {
		// 将className以驼峰规则加入下划线
		String tableName = CommonSqlProvider.getTableName(obj);
		List<String> columns = CommonSqlProvider.getTableColumns(obj);
		String tableKey = CommonSqlProvider.getKey(obj, false);
		if (columns.contains(tableKey) && CommonSqlProvider.hasNonEmptyValue(obj, tableKey)) {
			Long auditUid = LoginUserUtil.getLoginUserId();
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			for (String column : columns) {
				if (auditUid != null && CommonSqlProvider.isEditorAuditColumn(column)) {
					continue;
				}
				// getCreateColumns
				// 如果字段为null,切创建人字段不包含更新&& !jsonObject.get(column).equals("")
				if (CommonSqlProvider.prop(obj, column) != null && !CommonSqlProvider.isCreateAuditColumn(column)) {
					baseSQL.SET(CommonSqlProvider.rename(column) + "=#{" + column + "}");
				}
			}
			CommonSqlProvider.appendEditorAuditSet(baseSQL, columns, obj);
			baseSQL.WHERE(CommonSqlProvider.rename(tableKey) + "=#{" + tableKey + "}");
			String logicProp = CommonSqlProvider.getLogicDeleteField(obj, false);
			if (StringUtils.isNotBlank(logicProp) && columns.contains(logicProp)) {
				baseSQL.WHERE(CommonSqlProvider.rename(logicProp) + "=" + CommonSqlProvider.getLogicValidValue(obj));
			}
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}

	/**
	 * 生成 {@link org.peach.common.mybatis.mapper.BaseMapper#updateBaseAll} 的
	 * UPDATE：主键必填；非主键列含 null 也会写入。
	 *
	 * @param obj 含主键的实体
	 * @return 动态 SQL 字符串
	 * @throws org.peach.common.mvc.exception.BizException 无主键或主键为空时（{@link CrudBizCode#TABLE_KEY_INVALID}）
	 */
	public String updateBaseAllSQL(Object obj) {
		// 将className以驼峰规则加入下划线
		String tableName = CommonSqlProvider.getTableName(obj);
		List<String> columns = CommonSqlProvider.getTableColumns(obj);
		String tableKey = CommonSqlProvider.getKey(obj, false);
		if (columns.contains(tableKey) && CommonSqlProvider.hasNonEmptyValue(obj, tableKey)) {
			Long auditUid = LoginUserUtil.getLoginUserId();
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			for (String column : columns) {
				if (auditUid != null && CommonSqlProvider.isEditorAuditColumn(column)) {
					continue;
				}
				// getCreateColumns
				// 如果字段为null,且创建人字段不包含更新
				if (!column.equals(tableKey) && !CommonSqlProvider.isCreateAuditColumn(column)) {
					baseSQL.SET(CommonSqlProvider.rename(column) + "=#{" + column + "}");
				}
			}
			CommonSqlProvider.appendEditorAuditSet(baseSQL, columns, obj);
			baseSQL.WHERE(CommonSqlProvider.rename(tableKey) + "=#{" + tableKey + "}");
			String logicProp = CommonSqlProvider.getLogicDeleteField(obj, false);
			if (StringUtils.isNotBlank(logicProp) && columns.contains(logicProp)) {
				baseSQL.WHERE(CommonSqlProvider.rename(logicProp) + "=" + CommonSqlProvider.getLogicValidValue(obj));
			}
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}

	public String logicDeleteByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = CommonSqlProvider.getTableName(voClass);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		String logicDeleteField = CommonSqlProvider.getLogicDeleteField(voClass, false);
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		if (key != null && !"".equals(key) && columns.contains(logicDeleteField) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();

			baseSQL.UPDATE(tableName);
			baseSQL.SET(
					CommonSqlProvider.rename(logicDeleteField) + "=" + CommonSqlProvider.getLogicInvalidValue(voClass));
			CommonSqlProvider.appendEditorAuditSet(baseSQL, columns, voClass);
			baseSQL.WHERE(CommonSqlProvider.rename(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.LOGIC_DELETE_CONFIG_INVALID);
		}
	}

	public String logicRecoveryByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = CommonSqlProvider.getTableName(voClass);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		String logicDeleteField = CommonSqlProvider.getLogicDeleteField(voClass, false);
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		if (key != null && !"".equals(key) && columns.contains(logicDeleteField) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.UPDATE(tableName);
			baseSQL.SET(
					CommonSqlProvider.rename(logicDeleteField) + "=" + CommonSqlProvider.getLogicValidValue(voClass));
			CommonSqlProvider.appendEditorAuditSet(baseSQL, columns, voClass);
			baseSQL.WHERE(CommonSqlProvider.rename(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.LOGIC_DELETE_CONFIG_INVALID);
		}
	}

	public <U> String logicBatchDeleteKeysSQL(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass) {
		String tableName = CommonSqlProvider.getTableName(voClass);
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		String logicDeleteField = CommonSqlProvider.getLogicDeleteField(voClass, false);
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
			baseSQL.SET(
					CommonSqlProvider.rename(logicDeleteField) + "=" + CommonSqlProvider.getLogicInvalidValue(voClass));
			CommonSqlProvider.appendEditorAuditSet(baseSQL, columns, voClass);
			baseSQL.WHERE(CommonSqlProvider.rename(tableKey) + " IN (" + sb.toString() + ")");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.LOGIC_DELETE_CONFIG_INVALID);
		}
	}

}
