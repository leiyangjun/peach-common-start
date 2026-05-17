package org.peach.common.mybatis.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.CommentGeneratorConfiguration;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.config.JavaClientGeneratorConfiguration;
import org.mybatis.generator.config.JavaModelGeneratorConfiguration;
import org.mybatis.generator.config.ModelType;
import org.mybatis.generator.config.PluginConfiguration;
import org.mybatis.generator.config.TableConfiguration;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.peach.common.mybatis.generator.BaseMapperGeneratorRequest.TableSpec;

/**
 * 基于内存中的 MBG {@link Configuration} 生成 Entity 与 Mapper（配合
 * {@link MybatisGeneratorCustomPlugin}，不生成 XML），无需维护 {@code generatorConfig.xml}。
 * <p>
 * <b>推荐</b>：{@link #generate(String, String, String, String, Collection)} ——
 * 仅需连接信息 + 表名；在<strong>业务模块根目录</strong>下运行（使 {@code user.dir} 指向该模块），
 * 代码写入 {@code src/main/java}，包名为「调用本方法的类所在包」下的 {@code entity} 与
 * {@code mapper} 子包（实体 {@code xxx.entity}，接口 {@code xxx.mapper}）。
 * </p>
 * <p>
 * 注意：classpath 需包含对应 JDBC 驱动（如 {@code mysql-connector-j}）。
 * </p>
 */
public final class BaseMapperGeneratorUtil {

	private static final String SUB_ENTITY = "entity";
	private static final String SUB_MAPPER = "mapper";

	private BaseMapperGeneratorUtil() {
	}

	/**
	 * 简化入口：数据库 URL、用户名、密码、驱动类（可 {@code null} 则按 URL 推断）、一张或多张表名。
	 * <p>
	 * 输出目录：{@code Paths.get(System.getProperty("user.dir")).resolve("src/main/java")}。
	 * 实体包：{@code 调用类所在包 + ".entity"}，Mapper 包：{@code 调用类所在包 + ".mapper"}。
	 * </p>
	 *
	 * @param jdbcDriverClass 全限定驱动类名，{@code null} 或空白时按 {@link #inferJdbcDriverClass} 推断
	 * @param tableNames      至少一张表（库表名）；未指定 {@code domainObjectName} 时由 MBG 按表名规则生成类名
	 */
	public static List<String> generate(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, String... tableNames) {
		return generate(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriverClass,
				tableNames == null ? List.of() : Arrays.asList(tableNames));
	}

	/**
	 * 同 {@link #generate(String, String, String, String, String...)}，表名以集合传入。
	 */
	public static List<String> generate(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, Collection<String> tableNames) {
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
		String basePackage = resolveCallerBasePackage();
		Path javaRoot = Path.of(System.getProperty("user.dir")).resolve("src").resolve("main").resolve("java")
				.toAbsolutePath().normalize();
		return generate(toFullRequest(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriverClass, javaRoot, basePackage,
				names));
	}

	/**
	 * 将简化参数转为完整 {@link BaseMapperGeneratorRequest}（供同包测试或二次封装）。
	 */
	static BaseMapperGeneratorRequest toFullRequest(String jdbcUrl, String jdbcUsername, String jdbcPassword,
			String jdbcDriverClass, Path javaSourceRoot, String basePackage, List<String> tableNames) {
		BaseMapperGeneratorRequest.Builder b = BaseMapperGeneratorRequest.builder().jdbcUrl(jdbcUrl)
				.jdbcUsername(jdbcUsername).jdbcPassword(jdbcPassword == null ? "" : jdbcPassword)
				.jdbcDriverClass(jdbcDriverClass).entityPackage(basePackage + "." + SUB_ENTITY)
				.mapperPackage(basePackage + "." + SUB_MAPPER).javaSourceRoot(javaSourceRoot);
		for (String t : tableNames) {
			b.addTable(TableSpec.of(t));
		}
		return b.build();
	}

	/**
	 * 取「直接调用方」所在包名作为根包（如 {@code org.example.app} 则实体落在 {@code org.example.app.entity}）。
	 * <p>
	 * 会忽略本类及同包中的 {@code GeneratorUtil} 桥接类，使业务代码在调用简写入口时包名仍指向业务类。
	 * </p>
	 */
	public static String resolveCallerBasePackage() {
		final String utilBridge = "org.peach.common.mybatis.generator.GeneratorUtil";
		StackTraceElement[] st = new Throwable().getStackTrace();
		for (StackTraceElement e : st) {
			String cn = e.getClassName();
			if (cn.startsWith("java.") || cn.startsWith("jdk.") || cn.startsWith("sun.")
					|| cn.startsWith("org.junit.") || cn.startsWith("org.apache.maven.")) {
				continue;
			}
			if (cn.equals(BaseMapperGeneratorUtil.class.getName())) {
				continue;
			}
			if (cn.equals(utilBridge)) {
				continue;
			}
			try {
				Class<?> c = Class.forName(cn);
				String pkg = c.getPackageName();
				return StringUtils.isBlank(pkg) ? "generated" : pkg;
			} catch (ClassNotFoundException | NoClassDefFoundError ignored) {
				// 继续向上查找
			}
		}
		return "generated";
	}

	/**
	 * 根据 JDBC URL 与可选的显式驱动类名解析驱动；显式非空时直接使用。
	 *
	 * @param jdbcUrl           连接 URL
	 * @param jdbcDriverClass   可为 null 或空白
	 * @return 全限定驱动类名
	 */
	public static String inferJdbcDriverClass(String jdbcUrl, String jdbcDriverClass) {
		if (StringUtils.isNotBlank(jdbcDriverClass)) {
			return jdbcDriverClass.trim();
		}
		if (StringUtils.isBlank(jdbcUrl)) {
			throw new IllegalArgumentException("jdbcUrl 不能为空");
		}
		String u = jdbcUrl.toLowerCase();
		if (u.contains("jdbc:mariadb")) {
			return "org.mariadb.jdbc.Driver";
		}
		if (u.contains("jdbc:mysql")) {
			return "com.mysql.cj.jdbc.Driver";
		}
		if (u.contains("jdbc:postgresql")) {
			return "org.postgresql.Driver";
		}
		if (u.contains("jdbc:h2")) {
			return "org.h2.Driver";
		}
		if (u.contains("jdbc:oracle")) {
			return "oracle.jdbc.OracleDriver";
		}
		if (u.contains("jdbc:sqlserver") || u.contains("jdbc:microsoft:sqlserver")) {
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		}
		throw new IllegalArgumentException("无法从 JDBC URL 推断驱动，请传入 jdbcDriverClass（全限定驱动类名）");
	}

	/**
	 * 在已执行过 {@link #generate(BaseMapperGeneratorRequest)} 且实体与 Mapper 已写入 {@code javaSourceRoot} 后，
	 * 依据同一 {@link BaseMapperGeneratorRequest} 生成 VO、Service 接口、ServiceImpl、Controller（纯字符串，无 FreeMarker/模板文件）。
	 * <p>
	 * 由 {@code entityPackage} 反推根包，例如 {@code a.b.entity} -> 生成到 {@code a.b.vo}、{@code a.b.service} 等；{@code /api/xxx} 的
	 * 规则见 {@link BaseMapperMvcSourceGenerator#tableNameToDomainName} 与类名首字母小写。若 MBG 生成的类名与表名推断不一致，请在
	 * {@link org.peach.common.mybatis.generator.BaseMapperGeneratorRequest.TableSpec} 中显式传 {@code domainObjectName}。
	 * </p>
	 *
	 * @return 新写入的 Java 文件路径列表
	 */
	public static List<Path> generateMvcSource(BaseMapperGeneratorRequest request) {
		return BaseMapperMvcSourceGenerator.writeAll(request);
	}

	/**
	 * 执行生成，返回 MBG 警告列表（可能为空，不代表失败）。
	 *
	 * @param request 生成参数
	 * @return 警告信息（不可变列表）
	 */
	public static List<String> generate(BaseMapperGeneratorRequest request) {
		ObjectsRequire(request);
		List<String> warnings = new ArrayList<>();
		try {
			Configuration configuration = buildConfiguration(request);
			MyBatisGenerator generator = new MyBatisGenerator(configuration,
					new DefaultShellCallback(request.isOverwrite()), warnings);
			generator.generate(null);
			cleanupMapperImports(request);
			return Collections.unmodifiableList(new ArrayList<>(warnings));
		} catch (InvalidConfigurationException e) {
			throw new IllegalArgumentException("MyBatis Generator 配置无效: " + e.getErrors(), e);
		} catch (SQLException e) {
			throw new IllegalStateException("连接数据库或内省表结构失败", e);
		} catch (IOException e) {
			throw new IllegalStateException("写入生成文件失败", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("代码生成被中断", e);
		}
	}

	private static void ObjectsRequire(BaseMapperGeneratorRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request 不能为 null");
		}
	}

	/**
	 * 兼容 MyBatis3Simple 在空方法 Mapper 上仍输出冗余 import 的问题：按已生成表逐个清理。
	 */
	private static void cleanupMapperImports(BaseMapperGeneratorRequest request) throws IOException {
		Path javaRoot = Path.of(request.getJavaSourceRoot()).toAbsolutePath().normalize();
		Path mapperDir = javaRoot.resolve(request.getMapperPackage().replace('.', '/'));
		for (TableSpec spec : request.getTables()) {
			String domain = StringUtils.isNotBlank(spec.getDomainObjectName())
					? spec.getDomainObjectName().trim()
					: BaseMapperMvcSourceGenerator.tableNameToDomainName(spec.getTableName());
			Path mapperFile = mapperDir.resolve(domain + "Mapper.java");
			if (!Files.isRegularFile(mapperFile)) {
				continue;
			}
			String src = Files.readString(mapperFile);
			String cleaned = src
					.replace("import java.util.List;\n", "")
					.replace("import org.apache.ibatis.annotations.Delete;\n", "")
					.replace("import org.apache.ibatis.annotations.Insert;\n", "")
					.replace("import org.apache.ibatis.annotations.Result;\n", "")
					.replace("import org.apache.ibatis.annotations.Results;\n", "")
					.replace("import org.apache.ibatis.annotations.Select;\n", "")
					.replace("import org.apache.ibatis.annotations.Update;\n", "")
					.replace("import org.apache.ibatis.type.JdbcType;\n", "");
			if (!cleaned.equals(src)) {
				Files.writeString(mapperFile, cleaned);
			}
		}
	}

	/**
	 * 构建与 XML 配置等价的 {@link Configuration}，便于在测试中校验或二次扩展。
	 */
	public static Configuration buildConfiguration(BaseMapperGeneratorRequest request) {
		ObjectsRequire(request);
		String javaRoot = Path.of(request.getJavaSourceRoot()).toAbsolutePath().normalize().toString();
		String driverClass = inferJdbcDriverClass(request.getJdbcUrl(), request.getJdbcDriverClass());

		Context context = new Context(ModelType.FLAT);
		context.setId(request.getContextId());
		context.setTargetRuntime("MyBatis3Simple");
		context.addProperty("javaFileEncoding", request.getEncoding());

		CommentGeneratorConfiguration comment = new CommentGeneratorConfiguration();
		comment.addProperty("suppressAllComments", "true");
		context.setCommentGeneratorConfiguration(comment);

		JDBCConnectionConfiguration jdbc = new JDBCConnectionConfiguration();
		jdbc.setDriverClass(driverClass);
		jdbc.setConnectionURL(request.getJdbcUrl());
		jdbc.setUserId(request.getJdbcUsername());
		jdbc.setPassword(request.getJdbcPassword());
		context.setJdbcConnectionConfiguration(jdbc);

		JavaModelGeneratorConfiguration javaModel = new JavaModelGeneratorConfiguration();
		javaModel.setTargetPackage(request.getEntityPackage());
		javaModel.setTargetProject(javaRoot);
		context.setJavaModelGeneratorConfiguration(javaModel);

		JavaClientGeneratorConfiguration javaClient = new JavaClientGeneratorConfiguration();
		javaClient.setConfigurationType("ANNOTATEDMAPPER");
		javaClient.setTargetPackage(request.getMapperPackage());
		javaClient.setTargetProject(javaRoot);
		context.setJavaClientGeneratorConfiguration(javaClient);

		PluginConfiguration plugin = new PluginConfiguration();
		plugin.setConfigurationType(MybatisGeneratorCustomPlugin.class.getName());
		if (StringUtils.isNotBlank(request.getLogicDeleteColumn())) {
			plugin.addProperty("logicDeleteColumn", request.getLogicDeleteColumn());
		}
		plugin.addProperty("addMapperAnnotation", Boolean.toString(request.isAddMapperAnnotation()));
		plugin.addProperty("generateExample", Boolean.toString(request.isGenerateExample()));
		context.addPluginConfiguration(plugin);

		for (TableSpec spec : request.getTables()) {
			TableConfiguration table = new TableConfiguration(context);
			if (spec.getCatalog() != null) {
				table.setCatalog(spec.getCatalog());
			}
			if (spec.getSchema() != null) {
				table.setSchema(spec.getSchema());
			}
			table.setTableName(spec.getTableName());
			if (spec.getDomainObjectName() != null) {
				table.setDomainObjectName(spec.getDomainObjectName());
			}
			context.addTableConfiguration(table);
		}

		Configuration configuration = new Configuration();
		configuration.addContext(context);
		return configuration;
	}
}
