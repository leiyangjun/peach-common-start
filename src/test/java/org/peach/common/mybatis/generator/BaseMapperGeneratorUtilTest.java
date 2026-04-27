package org.peach.common.mybatis.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.peach.common.mybatis.generator.BaseMapperGeneratorRequest.TableSpec;

/**
 * {@link BaseMapperGeneratorUtil} 单元测试（不连真实数据库）。
 */
class BaseMapperGeneratorUtilTest {

	@Test
	void inferJdbcDriverClass_explicitWins() {
		assertThat(BaseMapperGeneratorUtil.inferJdbcDriverClass("jdbc:mysql://localhost/x", "org.example.Driver"))
				.isEqualTo("org.example.Driver");
	}

	@Test
	void inferJdbcDriverClass_fromMysqlUrl() {
		assertThat(BaseMapperGeneratorUtil.inferJdbcDriverClass("jdbc:mysql://127.0.0.1:3306/db", null))
				.isEqualTo("com.mysql.cj.jdbc.Driver");
	}

	@Test
	void inferJdbcDriverClass_fromMariaDbUrl() {
		assertThat(BaseMapperGeneratorUtil.inferJdbcDriverClass("jdbc:mariadb://127.0.0.1/db", null))
				.isEqualTo("org.mariadb.jdbc.Driver");
	}

	@Test
	void inferJdbcDriverClass_unknownUrl() {
		assertThatThrownBy(() -> BaseMapperGeneratorUtil.inferJdbcDriverClass("jdbc:unknown://x", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("无法从 JDBC URL 推断驱动");
	}

	@Test
	void toFullRequest_entityAndMapperSubpackages() throws Exception {
		BaseMapperGeneratorRequest req = BaseMapperGeneratorUtil.toFullRequest("jdbc:mysql://127.0.0.1/x", "u", "p", null,
				Path.of("target", "mbg-layout"), "com.acme.app", List.of("user", "order"));
		assertThat(req.getEntityPackage()).isEqualTo("com.acme.app.entity");
		assertThat(req.getMapperPackage()).isEqualTo("com.acme.app.mapper");
		assertThat(req.getTables()).hasSize(2);
		Configuration configuration = BaseMapperGeneratorUtil.buildConfiguration(req);
		configuration.validate();
	}

	@Test
	void buildConfiguration_structureOk() throws Exception {
		BaseMapperGeneratorRequest request = BaseMapperGeneratorRequest.builder()
				.jdbcUrl("jdbc:mysql://127.0.0.1:3306/demo")
				.jdbcUsername("root")
				.jdbcPassword("secret")
				.entityPackage("com.example.domain")
				.mapperPackage("com.example.mapper")
				.javaSourceRoot(Path.of("target", "mbg-test-src"))
				.addTable(TableSpec.of("user", "User"))
				.build();

		Configuration configuration = BaseMapperGeneratorUtil.buildConfiguration(request);
		configuration.validate();
		assertThat(configuration.getContexts()).hasSize(1);
	}

	@Test
	void requestBuild_rejectsEmptyTables() {
		assertThatThrownBy(() -> BaseMapperGeneratorRequest.builder()
				.jdbcUrl("jdbc:mysql://localhost/x")
				.jdbcUsername("u")
				.entityPackage("a")
				.mapperPackage("b")
				.javaSourceRoot("target/x")
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("至少");
	}

	@Test
	void myBatisGenerator_ctorValidatesEmptyTables() {
		Configuration configuration = new Configuration();
		assertThatThrownBy(configuration::validate).isInstanceOf(InvalidConfigurationException.class);
	}
}
