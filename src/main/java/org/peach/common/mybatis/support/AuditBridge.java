package org.peach.common.mybatis.support;

/**
 * 逻辑删除/恢复时可选填充修改人信息；默认空实现，业务可在启动时通过静态钩子扩展（避免强依赖用户会话框架）。
 *
 * @author leiyangjun
 */
public final class AuditBridge {

	private static volatile String userIdSupplier;
	private static volatile String userNameSupplier;

	private AuditBridge() {
	}

	/**
	 * 注册当前用户 ID 提供者（如从 SecurityContext 读取）。
	 */
	public static void setUserIdSupplier(String userId) {
		userIdSupplier = userId;
	}

	/**
	 * 注册当前用户名称提供者。
	 */
	public static void setUserNameSupplier(String userName) {
		userNameSupplier = userName;
	}

	public static String getUserId() {
		return userIdSupplier != null ? userIdSupplier : "";
	}

	public static String getUserName() {
		return userNameSupplier != null ? userNameSupplier : "";
	}
}

