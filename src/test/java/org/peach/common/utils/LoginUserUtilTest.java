package org.peach.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.peach.common.security.UserType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link LoginUserUtil#getLoginUser} 与网关注入查询参数约定一致。
 */
class LoginUserUtilTest {

	@AfterEach
	void resetContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void getLoginUser_readsPeachParams() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setParameter(LoginUserUtil.QUERY_USER_ID, "42");
		req.setParameter(LoginUserUtil.QUERY_USER_TYPE, UserType.APP);
		req.setParameter(LoginUserUtil.QUERY_USERNAME, "alice");
		req.setParameter(LoginUserUtil.QUERY_NICKNAME, "艾丽丝");
		req.setParameter(LoginUserUtil.QUERY_REAL_NAME, "王五");
		req.setParameter(LoginUserUtil.QUERY_MOBILE, "13800138000");
		req.setParameter(LoginUserUtil.QUERY_EMAIL, "a@example.com");
		req.setParameter(LoginUserUtil.QUERY_AVATAR, "https://cdn.example/avatar.png");
		req.setParameter(LoginUserUtil.QUERY_GENDER, "1");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
		var u = LoginUserUtil.getLoginUser();
		assertThat(u).isNotNull();
		assertThat(u.getId()).isEqualTo(42L);
		assertThat(u.getUserType()).isEqualTo(UserType.APP);
		assertThat(u.getUsername()).isEqualTo("alice");
		assertThat(u.getNickname()).isEqualTo("艾丽丝");
		assertThat(u.getRealName()).isEqualTo("王五");
		assertThat(u.getMobile()).isEqualTo("13800138000");
		assertThat(u.getEmail()).isEqualTo("a@example.com");
		assertThat(u.getAvatar()).isEqualTo("https://cdn.example/avatar.png");
		assertThat(u.getGender()).isEqualTo((short) 1);
	}

	@Test
	void getLoginUser_nullWhenNoIdentity() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
		assertThat(LoginUserUtil.getLoginUser()).isNull();
	}
}
