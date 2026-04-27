package org.peach.common.mybatis.generator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * 一站式代码生成入口：依次执行 {@link BaseMapperGeneratorUtil#generate}（Entity + Mapper）与
 * {@link BaseMapperGeneratorUtil#generateMvcSource}（VO、Service、ServiceImpl、Controller），
 * 无需在业务侧组装 {@link BaseMapperGeneratorRequest}。
 * <p>
 * 推荐在<strong>业务模块根目录</strong>下从业务类中调用，使「根包名」为业务类所在包（同
 * {@link BaseMapperGeneratorUtil#resolveCallerBasePackage}）；也可使用显式传入
 * {@code javaSourceRoot + basePackage} 的重载。
 * </p>
 *
 * @author leiyangjun
 */
public final class GeneratorUtil {

	private GeneratorUtil() {
	}

	/**
	 * 同 {@link #generateAll(String, String, String, String, Collection)}，表名以变参传入。
	 *
	 * @param jdbcDriverClass 可 {@code null}，将按 URL 由 {@link BaseMapperGeneratorUtil#inferJdbcDriverClass} 推断
	 */
	public static GeneratorResult generateAll(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, String... tableNames) {
		return generateAll(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriverClass,
				tableNames == null ? List.of() : Arrays.asList(tableNames));
	}

	/**
	 * 一步生成 Entity、Mapper 及 MVC 分层代码。输出到当前模块
	 * {@code user.dir + /src/main/java}，包名为调用类所在包。
	 */
	public static GeneratorResult generateAll(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, Collection<String> tableNames) {
		List<String> names = collectTableNames(tableNames);
		String base = BaseMapperGeneratorUtil.resolveCallerBasePackage();
		Path javaRoot = defaultJavaSourceRoot();
		return doGenerateAll(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriverClass, javaRoot, base, names);
	}

	/**
	 * 与 {@link #generateAll(String, String, String, String, Collection)} 相同，但由调用方显式指定源码根与根包，不依赖
	 * 栈上溯（适合在 CI、IDE 多模块或自定义布局下调用）。
	 *
	 * @param javaSourceRoot 一般指向 {@code .../src/main/java} 的绝对或规范路径
	 * @param basePackage    业务根包，实体将落在 {@code basePackage.entity}，Mapper 在 {@code basePackage.mapper}
	 */
	public static GeneratorResult generateAll(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, Path javaSourceRoot, String basePackage, String... tableNames) {
		return generateAll(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriverClass, javaSourceRoot, basePackage,
				tableNames == null ? List.of() : Arrays.asList(tableNames));
	}

	/**
	 * 同 {@link #generateAll(String, String, String, String, Path, String, String...)}，表名以集合传入。
	 */
	public static GeneratorResult generateAll(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, Path javaSourceRoot, String basePackage, Collection<String> tableNames) {
		List<String> names = collectTableNames(tableNames);
		if (StringUtils.isBlank(basePackage)) {
			throw new IllegalArgumentException("basePackage 不能为空");
		}
		Path javaRoot = javaSourceRoot == null ? defaultJavaSourceRoot() : javaSourceRoot.toAbsolutePath().normalize();
		return doGenerateAll(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriverClass, javaRoot, basePackage.trim(), names);
	}

	private static GeneratorResult doGenerateAll(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, Path javaSourceRoot, String basePackage, List<String> tableNames) {
		BaseMapperGeneratorRequest req = BaseMapperGeneratorUtil.toFullRequest(jdbcUrl, jdbcUsername, jdbcPassword,
				jdbcDriverClass, javaSourceRoot, basePackage, tableNames);
		List<String> warnings = BaseMapperGeneratorUtil.generate(req);
		List<Path> mvc = BaseMapperGeneratorUtil.generateMvcSource(req);
		return new GeneratorResult(warnings, mvc);
	}

	private static List<String> collectTableNames(Collection<String> tableNames) {
		if (tableNames == null || tableNames.isEmpty()) {
			throw new IllegalArgumentException("至少指定一张表");
		}
		List<String> names = new ArrayList<>();
		for (String t : tableNames) {
			if (StringUtils.isNotBlank(t)) {
				names.add(t.trim());
			}
		}
		if (names.isEmpty()) {
			throw new IllegalArgumentException("至少指定一张表");
		}
		return names;
	}

	private static Path defaultJavaSourceRoot() {
		return Path.of(System.getProperty("user.dir")).resolve("src").resolve("main").resolve("java")
				.toAbsolutePath().normalize();
	}
}
