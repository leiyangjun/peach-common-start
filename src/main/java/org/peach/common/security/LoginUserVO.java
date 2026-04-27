package org.peach.common.security;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户视图对象：从<strong>网关注入的查询参数</strong>（经 {@link org.peach.common.utils.CurrentLoginUserUtil}）解析出的<strong>非敏感</strong>展示字段，
 * 供接口返回或业务逻辑读取；<strong>不包含密码、令牌等机密</strong>。
 * <p>
 * 数据来源与约定：
 * </p>
 * <ul>
 * <li>典型由 {@link org.peach.common.utils.CurrentLoginUserUtil#fromGatewayQueryParams} 从请求参数构建，不访问数据库；</li>
 * <li>业务可在拿到快照后调用服务层，将库中最新昵称、手机、头像等合并进本对象（见
 * {@link org.peach.common.utils.CurrentLoginUserUtil#mergeProfile}）；</li>
 * <li>与持久化实体字段的对应关系为逻辑映射：{@code id} 对应网关参数 {@code peach_user_id}（用户主键），
 * {@code subjectType} 对应 {@code peach_subject_type}，取值须符合 {@link UserType}。</li>
 * </ul>
 * <p>
 * 命名说明：后缀 VO 表示面向接口层的视图数据，与代码生成产生的业务 {@code XxxVO} 无继承关系，仅命名习惯一致。
 * </p>
 *
 * @author leiyangjun
 */
public class LoginUserVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 用户主键：与 JWT 标准 Claim {@code sub} 一致，一般为雪花或数值型主键的字符串形式解析结果。
	 */
	private Long id;

	/**
	 * 登录名：优先来自 Claim {@code preferred_username}，部分实现回退 {@code user_name}。
	 */
	private String username;

	/**
	 * 用户类型：与库表 {@code subject_type} 及 Claim {@code subject_type} 对齐，取值为 {@link UserType} 中常量。
	 */
	private String subjectType;

	/**
	 * 昵称：可选；可由授权服务器在签发令牌时写入 Claim {@code nickname}，也可由业务合并库表后覆盖。
	 */
	private String nickname;

	/**
	 * 手机号：可选；通常不在 JWT 中携带，由业务合并库表资料后填充。
	 */
	private String mobile;

	/**
	 * 头像 URL：可选；通常由业务合并库表后填充。
	 */
	private String avatarUrl;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSubjectType() {
		return subjectType;
	}

	public void setSubjectType(String subjectType) {
		this.subjectType = subjectType;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}
}
