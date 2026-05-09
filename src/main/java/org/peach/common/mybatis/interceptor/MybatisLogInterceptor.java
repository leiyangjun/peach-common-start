package org.peach.common.mybatis.interceptor;

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
 * MyBatis SQL 语句日志拦截器：在 {@link StatementHandler#prepare} 阶段将已绑定的 SQL 输出到
 * SLF4J（logger {@code org.peach.mybatis.devsql}），便于与控制台 {@code StdOutImpl} 互补排查。
 * 严格生产环境（pro / produce / product）下由 {@link org.peach.common.mybatis.autoconfigure.MybatisLogAutoConfiguration}
 * 不注册本插件。
 *
 * @author leiyangjun
 */
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = { java.sql.Connection.class,
		Integer.class }))
public class MybatisLogInterceptor implements Interceptor {

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
