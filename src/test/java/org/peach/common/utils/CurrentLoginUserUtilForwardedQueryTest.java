package org.peach.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.peach.common.security.UserType;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link CurrentLoginUserUtil#fromGatewayQueryParams} 与网关注入查询参数约定一致。
 */
class CurrentLoginUserUtilForwardedQueryTest {

	@Test
	void fromGatewayQueryParams_readsPeachParams() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setParameter(CurrentLoginUserUtil.QUERY_USER_ID, "42");
		req.setParameter(CurrentLoginUserUtil.QUERY_USERNAME, "alice");
		req.setParameter(CurrentLoginUserUtil.QUERY_SUBJECT_TYPE, UserType.INTERNAL);
		assertThat(CurrentLoginUserUtil.fromGatewayQueryParams(req)).hasValueSatisfying(u -> {
			assertThat(u.getId()).isEqualTo(42L);
			assertThat(u.getUsername()).isEqualTo("alice");
			assertThat(u.getSubjectType()).isEqualTo(UserType.INTERNAL);
		});
	}

	@Test
	void fromGatewayQueryParams_emptyWhenNoIdentity() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		assertThat(CurrentLoginUserUtil.fromGatewayQueryParams(req)).isEmpty();
	}
}
