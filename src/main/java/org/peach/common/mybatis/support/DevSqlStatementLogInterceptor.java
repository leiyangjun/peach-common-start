package org.peach.common.mybatis.support;

import java.util.Properties;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 非严格生产环境下注册的语句拦截器：在 prepare 阶段将 SQL 打到 SLF4J（logger 名 {@code org.peach.mybatis.devsql}），
 * 不依赖 MyBatis {@code logImpl} 与各级 {@code logging.level}，便于本地排查「只见 SqlSession 不见 SQL」的情况。
 */
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = { java.sql.Connection.class,
		Integer.class }))
public class DevSqlStatementLogInterceptor implements Interceptor {

	private static final Logger SQL_LOG = LoggerFactory.getLogger("org.peach.mybatis.devsql");

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (invocation.getTarget() instanceof StatementHandler handler) {
			BoundSql boundSql = handler.getBoundSql();
			String compact = boundSql.getSql() == null ? "" : boundSql.getSql().replaceAll("\\s+", " ").trim();
			SQL_LOG.info("[Peach-Dev-SQL] {}", compact);
			if (SQL_LOG.isDebugEnabled()) {
				SQL_LOG.debug("[Peach-Dev-SQL] parameterObject={}", boundSql.getParameterObject());
			}
		}
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	@Override
	public void setProperties(Properties properties) {
	}
}
