package org.peach.common.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.type.TypeHandler;

/**
 * 标注在实体表字段上，供 {@link org.peach.common.mybatis.mapper.InsertSqlProvider} 等动态 SQL
 * 生成 {@code #{prop,typeHandler=...}}，以便 PostgreSQL {@code jsonb} 等非默认 JDBC 映射正确读写。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlTypeHandler {

	/**
	 * 对应 MyBatis {@link TypeHandler} 实现类全名会拼入占位符。
 *
 * @author leiyangjun
 */
	Class<? extends TypeHandler<?>> value();
}
