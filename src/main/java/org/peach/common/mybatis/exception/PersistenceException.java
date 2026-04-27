package org.peach.common.mybatis.exception;

/**
 * MyBatis 动态 SQL 构建或持久化约定不满足时抛出。
 *
 * @author leiyangjun
 */
public class PersistenceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PersistenceException(String message) {
		super(message);
	}

	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}

