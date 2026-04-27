package org.peach.common.mybatis.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link GeneratorUtil} 参数与返回结构校验；不连库，不跑完整 MBG + MVC 生成链。
 */
class GeneratorUtilTest {

	@Test
	void 至少一张表() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> GeneratorUtil.generateAll("jdbc:h2:mem:a", "sa", "", null, List.of()));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> GeneratorUtil.generateAll("jdbc:h2:mem:a", "sa", "", null, (String[]) null));
	}

	@Test
	@DisplayName("显式根包不能为空")
	void blankBasePackageRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> GeneratorUtil.generateAll("jdbc:h2:mem:a", "sa", "", null,
						Paths.get("/tmp"), "  ", "T_USER"));
	}

	@Test
	void 结果列表不可被外部误改_防御性复制() {
		GeneratorResult r = new GeneratorResult(List.of("w1"), List.of(Paths.get("a.java")));
		assertThat(r.mybatisGeneratorWarnings()).hasSize(1);
	}
}
