package org.peach.common.mybatis.mapper;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mvc.exception.BizException;
import org.peach.common.mybatis.code.CrudBizCode;
import org.springframework.util.CollectionUtils;

/**
 * {@link BaseMapper} 物理删除相关 {@link org.apache.ibatis.annotations.DeleteProvider}：按条件、主键或主键集合生成
 * {@code DELETE} 语句。
 *
 * @author leiyangjun
 */
public class DeleteSqlProvider {

	public String deleteBaseSQL(Object obj) {
		// 将className以驼峰规则加入下划线
		String tableName = CommonSqlProvider.getTableName(obj);
		List<String> columns = CommonSqlProvider.getTableColumns(obj);
		SQL baseSQL = new SQL();
		baseSQL.DELETE_FROM(tableName);
		for (String column : columns) {
			if (CommonSqlProvider.prop(obj, column) != null) {// 如果字段为null,不计入此处操作
				baseSQL.WHERE(CommonSqlProvider.sqlColumnName(column) + "=" + CommonSqlProvider.mybatisRootParam(obj, column));
			}
		}
		return baseSQL.toString();
	}

	public <T> String batchDeleteBaseSQL(List<T> list) {
		T t = list.get(0);
		String tableName = CommonSqlProvider.getTableName(t);
		String tableKey = CommonSqlProvider.getKey(t, false);
		List<String> columns = CommonSqlProvider.getTableColumns(t);
		StringBuilder sb = new StringBuilder();
		MessageFormat mf = new MessageFormat("#'{'list[{0,number,#}]." + tableKey + "}");
		boolean isReturn = true;
		if (!CollectionUtils.isEmpty(list)) {
			for (int i = 0; i < list.size(); i++) {
				sb.append(mf.format(new Object[] { i }));
				if (i < list.size() - 1) {
					sb.append(",");
				}
				if (CommonSqlProvider.isEmptyKeyValue(CommonSqlProvider.prop(list.get(i), tableKey))) {
					isReturn = false;
				}
			}
		}
		isReturn = CollectionUtils.isEmpty(list) ? false : isReturn;
		if (isReturn && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.DELETE_FROM(tableName);
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + " IN (" + sb.toString() + ")");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}

	public <U> String batchDeleteBaseByKeysSQL(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass) {
		String tableName = CommonSqlProvider.getTableName(voClass);
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
			baseSQL.DELETE_FROM(tableName);
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + " IN (" + sb.toString() + ")");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}

	public String deleteBaseByKeySQL(@Param("key") Serializable key, @Param("voClass") Class<?> voClass) {
		String tableName = CommonSqlProvider.getTableName(voClass);
		String tableKey = CommonSqlProvider.getKey(voClass, false);
		List<String> columns = CommonSqlProvider.getTableColumns(voClass);
		if (key != null && !"".equals(key) && columns.contains(tableKey)) {
			SQL baseSQL = new SQL();
			baseSQL.DELETE_FROM(tableName);
			baseSQL.WHERE(CommonSqlProvider.sqlColumnName(tableKey) + "=#{key}");
			return baseSQL.toString();
		} else {
			throw BizException.validWarn(CrudBizCode.TABLE_KEY_INVALID);
		}
	}

}
