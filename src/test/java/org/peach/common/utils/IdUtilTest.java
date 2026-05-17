package org.peach.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * {@link IdUtil} 公开方法行为：唯一性、长度区间、短雪花与标准雪花数值差异。
 */
class IdUtilTest {

	@Test
	void snowId_producesUniquePositiveLongs() {
		Set<Long> seen = new HashSet<>();
		for (int i = 0; i < 500; i++) {
			long id = IdUtil.snowId();
			assertThat(id).isPositive();
			assertThat(String.valueOf(id)).hasSizeBetween(17, 20);
			assertThat(seen.add(id)).as("第 %d 次重复", i).isTrue();
		}
	}

	@Test
	void shortSnowId_producesUniquePositiveLongsShorterThanStandard() {
		Set<Long> seen = new HashSet<>();
		for (int i = 0; i < 500; i++) {
			long shortId = IdUtil.shortSnowId();
			long standardId = IdUtil.snowId();
			assertThat(shortId).isPositive();
			assertThat(String.valueOf(shortId).length())
					.isLessThan(String.valueOf(standardId).length());
			assertThat(String.valueOf(shortId)).hasSizeBetween(12, 16);
			assertThat(seen.add(shortId)).as("第 %d 次重复", i).isTrue();
		}
	}

	@Test
	void shortSnowId_differsFromSnowIdOnSameCall() {
		long standard = IdUtil.snowId();
		long shortSnow = IdUtil.shortSnowId();
		assertThat(shortSnow).isNotEqualTo(standard);
	}

	@Test
	void shortId22_fixedLength() {
		assertThat(IdUtil.shortId22()).hasSize(22);
	}
}
