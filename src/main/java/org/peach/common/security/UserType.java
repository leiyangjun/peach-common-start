package org.peach.common.security;

/**
 * 用户类型（业务分域）：与持久层用户表字段 {@code user_type} 取值域一致。
 * <p>
 * 设计说明：
 * </p>
 * <ul>
 * <li>同一套账号表可按类型区分运营侧与 App 端，便于权限模型与审计；</li>
 * <li>新增类型时需同步数据库 CHECK 约束、本类常量及签发令牌处的校验逻辑；</li>
 * <li>本类仅承载<strong>字符串常量</strong>，如需强类型枚举可在业务层再包装。</li>
 * </ul>
 *
 * @author leiyangjun
 */
public final class UserType {

	/**
	 * 系统侧用户：后台管理、内部员工、运营账号等。
	 * <p>
	 * 约束：该类型下登录名在库中应非空（由表级 CHECK 保证）。
	 * </p>
	 */
	public static final String SYSTEM = "system";

	/**
	 * 应用端用户：小程序、App、开放场景终端用户等；允许以手机号、第三方标识等为主键登录路径，{@code username} 列可为空。
	 */
	public static final String APP = "app";

	private UserType() {
	}
}
