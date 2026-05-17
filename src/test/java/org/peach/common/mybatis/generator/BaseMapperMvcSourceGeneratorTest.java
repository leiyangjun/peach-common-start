package org.peach.common.mybatis.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.peach.common.mybatis.generator.BaseMapperGeneratorRequest.TableSpec;


/**
 * 不连库：预置实体源文件，再调 {@link BaseMapperGeneratorUtil#generateMvcSource}。
 */
class BaseMapperMvcSourceGeneratorTest {

	@TempDir
	Path javaRoot;

	@Test
	void generateMvcSource_writesFourFiles() throws Exception {
		String base = "com.acme.demo";
		String entP = base + ".entity";
		Files.createDirectories(javaRoot.resolve(entP.replace('.', '/')));
		String userBody = "package " + entP + ";\n" + "import java.io.Serializable;\n" + "public class User implements Serializable {\n"
				+ "  private static final long serialVersionUID = 1L;\n" + "  private String id;\n" + "  private String name;\n" + "}\n";
		Files.writeString(javaRoot.resolve(entP.replace('.', '/')).resolve("User.java"), userBody);
		Files.createDirectories(javaRoot.resolve((base + ".mapper").replace('.', '/')));
		String mapBody = "package " + base + ".mapper;\n" + "import org.peach.common.mybatis.mapper.BaseMapper;\n" + "import " + entP
				+ ".User;\n" + "public interface UserMapper extends BaseMapper<User> { }\n";
		Files.writeString(javaRoot.resolve((base + ".mapper").replace('.', '/')).resolve("UserMapper.java"), mapBody);

		BaseMapperGeneratorRequest req = BaseMapperGeneratorRequest.builder()
				.jdbcUrl("jdbc:mysql://127.0.0.1/x")
				.jdbcUsername("u")
				.jdbcPassword("p")
				.entityPackage(entP)
				.mapperPackage(base + ".mapper")
				.javaSourceRoot(javaRoot)
				.addTable(TableSpec.of("t_user", "User"))
				.build();

		List<Path> out = BaseMapperGeneratorUtil.generateMvcSource(req);
		assertThat(out).hasSize(4);
		Path vo = fileNamed(out, "UserVO.java");
		Path svc = fileNamed(out, "UserService.java");
		Path impl = fileNamed(out, "UserServiceImpl.java");
		Path ctl = fileNamed(out, "UserController.java");
		assertThat(Files.isRegularFile(vo)).isTrue();
		assertThat(vo).isEqualTo(javaRoot.resolve("com").resolve("acme").resolve("demo").resolve("vo").resolve("UserVO.java"));
		assertThat(Files.readString(svc)).contains("extends BaseInterfaceService<UserVO>");
		assertThat(Files.readString(impl))
				.contains("extends BaseAbstractService<UserMapper, User, UserVO>", "implements UserService", "@Service");
		assertThat(Files.readString(ctl))
				.contains("extends BaseController<UserVO, UserService>", "UserService service", "@RestController")
				.doesNotContain("UserServiceImpl");
	}

	private static Path fileNamed(List<Path> paths, String fileName) {
		return paths.stream()
				.filter(p -> Objects.equals(p.getFileName().toString(), fileName))
				.findFirst()
				.orElseThrow(() -> new AssertionError("未找到生成文件: " + fileName));
	}
}
