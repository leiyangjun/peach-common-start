package org.peach.common.mybatis.autoconfigure;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 创作日期：2026-04-26，作者：Codex
 * 按事务只读标记进行数据源路由：readOnly=true -> READ，其余 -> WRITE。
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

	public static final String WRITE_KEY = "WRITE";
	public static final String READ_KEY = "READ";

	@Override
	protected Object determineCurrentLookupKey() {
		return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? READ_KEY : WRITE_KEY;
	}
}

