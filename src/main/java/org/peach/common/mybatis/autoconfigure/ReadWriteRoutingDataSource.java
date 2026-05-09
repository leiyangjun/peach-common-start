package org.peach.common.mybatis.autoconfigure;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 基于 Spring 事务同步的读写数据源路由：当前事务为只读时返回 {@link #READ_KEY}，否则返回 {@link #WRITE_KEY}，
 * 与 {@link ReadWriteDataSourceAutoConfiguration} 中注册的目标数据源键一致。
 *
 * @author leiyangjun
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

	public static final String WRITE_KEY = "WRITE";
	public static final String READ_KEY = "READ";

	@Override
	protected Object determineCurrentLookupKey() {
		return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? READ_KEY : WRITE_KEY;
	}
}

