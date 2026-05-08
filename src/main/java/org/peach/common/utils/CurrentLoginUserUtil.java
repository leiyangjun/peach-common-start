package org.peach.common.utils;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.security.LoginUserVO;
import org.peach.common.security.UserType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前登录用户工具类：从当前 Servlet 请求的<strong>查询参数</strong>读取网关注入的身份快照（本 Starter 不引入 OAuth2）。
 * <p>
 * 参数名与 {@code peach-gateway} 中 {@code org.peach.gateway.security.UserForwardingGlobalFilter} 写入下游查询字符串的约定一致。
 * 业务微服务应部署在隔离网络中，仅接受来自网关的流量，避免终端伪造查询参数。
 * </p>
 * <p>
 * 与授权服务器签发令牌相关的 Claim 名 {@link #CLAIM_SUBJECT_TYPE} 仍供授权服务工程在定制 JWT 时使用，与本类读取查询参数无关。
 * </p>
 * <p>
 * 本工具类<strong>不查询数据库</strong>；需要库中最新资料时请在上层服务中调用
 * {@link #mergeProfile(LoginUserVO, String, String, String)} 合并。
 * </p>
 *
 * @author leiyangjun
 */
public final class CurrentLoginUserUtil {

	/**
	 * JWT / 令牌中存放用户类型的 Claim 名称（供授权服务器侧定制 Token 时使用）；值须与 {@link UserType} 中常量一致。
	 */
	public static final String CLAIM_SUBJECT_TYPE = "subject_type";

	/** 网关注入：用户主键，与令牌 {@code sub} 含义一致。 */
	public static final String QUERY_USER_ID = "peach_user_id";

	/** 网关注入：登录名。 */
	public static final String QUERY_USERNAME = "peach_username";

	/** 网关注入：主体类型，取值域见 {@link UserType}。 */
	public static final String QUERY_SUBJECT_TYPE = "peach_subject_type";

	private CurrentLoginUserUtil() {
	}

	/**
	 * 若当前线程存在 Servlet 请求，则根据网关注入的查询参数构造 {@link LoginUserVO}；否则返回空。
	 */
	public static Optional<LoginUserVO> currentOptional() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		if (ra instanceof ServletRequestAttributes servletAttrs) {
			return fromGatewayQueryParams(servletAttrs.getRequest());
		}
		return Optional.empty();
	}

	/**
	 * 从网关追加在请求 URI 上的查询参数构造 {@link LoginUserVO}；至少需存在用户 id 或用户名之一。
	 */
	public static Optional<LoginUserVO> fromGatewayQueryParams(HttpServletRequest request) {
		if (request == null) {
			return Optional.empty();
		}
		String idStr = request.getParameter(QUERY_USER_ID);
		String username = request.getParameter(QUERY_USERNAME);
		String subjectType = request.getParameter(QUERY_SUBJECT_TYPE);
		if (StringUtils.isAllBlank(idStr, username)) {
			return Optional.empty();
		}
		LoginUserVO u = new LoginUserVO();
		if (StringUtils.isNotBlank(idStr)) {
			try {
				u.setId(Long.parseLong(idStr.trim()));
			} catch (NumberFormatException ignored) {
				// 非数字 id 时仍可使用 username
			}
		}
		u.setUsername(username);
		u.setSubjectType(subjectType);
		return Optional.of(u);
	}

	/**
	 * 同 {@link #currentOptional()}，无有效身份参数时抛出 {@link AuthenticationCredentialsNotFoundException}。
	 */
	public static LoginUserVO currentOrThrow() {
		return currentOptional()
			.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("未携带网关注入的用户身份参数"));
	}

	/**
	 * 将库表或外部来源的用户展示字段合并到已有快照（非 null 且非空白才覆盖）。
	 *
	 * @param base      通常为 {@link #currentOrThrow()} 的结果，不可为 null
	 * @param nickname  昵称，可 null
	 * @param mobile    手机号，可 null
	 * @param avatarUrl 头像地址，可 null
	 */
	public static void mergeProfile(LoginUserVO base, String nickname, String mobile, String avatarUrl) {
		if (base == null) {
			return;
		}
		if (StringUtils.isNotBlank(nickname)) {
			base.setNickname(nickname);
		}
		if (StringUtils.isNotBlank(mobile)) {
			base.setMobile(mobile);
		}
		if (StringUtils.isNotBlank(avatarUrl)) {
			base.setAvatarUrl(avatarUrl);
		}
	}
}
