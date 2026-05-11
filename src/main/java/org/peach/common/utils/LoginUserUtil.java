package org.peach.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.peach.common.mvc.vo.LoginUserVO;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录用户工具：从当前线程绑定的 Servlet 请求中读取网关注入的查询参数（由 JWT {@code sub} 在网关展开），不访问数据库。
 */
public final class LoginUserUtil {

	/** 网关注入：用户主键 */
	public static final String QUERY_USER_ID = "peach_user_id";

	/** 网关注入：用户类别（system / app） */
	public static final String QUERY_USER_TYPE = "peach_user_type";

	/** 网关注入：登录名 */
	public static final String QUERY_USERNAME = "peach_username";

	/** 网关注入：昵称 */
	public static final String QUERY_NICKNAME = "peach_nickname";

	/** 网关注入：真实姓名 */
	public static final String QUERY_REAL_NAME = "peach_real_name";

	/** 网关注入：手机号 */
	public static final String QUERY_MOBILE = "peach_mobile";

	/** 网关注入：邮箱 */
	public static final String QUERY_EMAIL = "peach_email";

	/** 网关注入：头像 */
	public static final String QUERY_AVATAR = "peach_avatar";

	/** 网关注入：性别（0/1/2） */
	public static final String QUERY_GENDER = "peach_gender";

	private LoginUserUtil() {
	}

	/**
	 * 获取当前请求对应的登录用户快照；非 Web 上下文、未完成网关注入或缺少身份参数时返回 {@code null}。
	 */
	@Nullable
	public static LoginUserVO getLoginUser() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		if (!(ra instanceof ServletRequestAttributes servletAttrs)) {
			return null;
		}
		HttpServletRequest request = servletAttrs.getRequest();
		if (request == null) {
			return null;
		}
		String idStr = request.getParameter(QUERY_USER_ID);
		String username = request.getParameter(QUERY_USERNAME);
		String mobile = request.getParameter(QUERY_MOBILE);
		if (StringUtils.isAllBlank(idStr)) {
			return null;
		}
		LoginUserVO u = new LoginUserVO();
		if (StringUtils.isNotBlank(idStr)) {
			try {
				u.setId(Long.parseLong(idStr.trim()));
			} catch (NumberFormatException ignored) {
				// 忽略非法 id，仍可依赖用户名/手机
			}
		}
		u.setUsername(username);
		u.setUserType(request.getParameter(QUERY_USER_TYPE));
		u.setNickname(request.getParameter(QUERY_NICKNAME));
		u.setRealName(request.getParameter(QUERY_REAL_NAME));
		u.setMobile(mobile);
		u.setEmail(request.getParameter(QUERY_EMAIL));
		u.setAvatar(request.getParameter(QUERY_AVATAR));
		String g = request.getParameter(QUERY_GENDER);
		if (StringUtils.isNotBlank(g)) {
			try {
				u.setGender(Short.parseShort(g.trim()));
			} catch (NumberFormatException ignored) {
				// 性别解析失败则保持 null
			}
		}
		return u;
	}

	/**
	 * 
	 * @Title: getLoginUserId
	 * @Description: 获取当前登陆用户ID
	 * @param: @return
	 * @return: Long
	 * @throws
	 */
	public static Long getLoginUserId() {
		LoginUserVO loginUserVO = getLoginUser();
		return loginUserVO == null ? null : loginUserVO.getId();
	}

	/**
	 * 当前登录用户昵称（来自网关注入 {@link #QUERY_NICKNAME}）；无登录上下文或昵称为空时返回 {@code null}。
	 */
	@Nullable
	public static String getLoginUserNickname() {
		LoginUserVO u = getLoginUser();
		if (u == null || StringUtils.isBlank(u.getNickname())) {
			return null;
		}
		return u.getNickname().trim();
	}

}
