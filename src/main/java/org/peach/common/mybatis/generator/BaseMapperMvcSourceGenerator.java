package org.peach.common.mybatis.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mybatis.generator.BaseMapperGeneratorRequest.TableSpec;

/**
 * 在 MBG 已落盘 Entity/Mapper 后，用纯 Java 字符串生成 VO、Service 接口、ServiceImpl、Controller。
 * 从实体源文件中解析非 static 的 {@code private} 字段；import 自实体中复制并过滤与持久化/Mapper 相关项。
 */
final class BaseMapperMvcSourceGenerator {

	/** 单行 {@code private} 字段；若实体字段上一行有注解，可能需手调。 */
	private static final Pattern PRIVATE_FIELD = Pattern.compile("private\\s+(.+?)\\s+(\\w+)\\s*;");

	private BaseMapperMvcSourceGenerator() {
	}

	static List<Path> writeAll(BaseMapperGeneratorRequest request) {
		try {
			Path javaRoot = Path.of(request.getJavaSourceRoot()).toAbsolutePath().normalize();
			String appBase = baseFromEntityPackage(request.getEntityPackage());
			List<Path> all = new ArrayList<>();
			for (TableSpec spec : request.getTables()) {
				String domain = StringUtils.isNotBlank(spec.getDomainObjectName()) ? spec.getDomainObjectName().trim()
						: tableNameToDomainName(spec.getTableName());
				String mapping = requestMappingFor(domain);
				all.addAll(writeOneTable(javaRoot, request.getEntityPackage(), request.getMapperPackage(), appBase, domain, mapping));
			}
			return all;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String baseFromEntityPackage(String entityPackage) {
		if (entityPackage.endsWith(".entity")) {
			return entityPackage.substring(0, entityPackage.length() - ".entity".length());
		}
		return entityPackage;
	}

	/** 与 MBG 常见习惯一致：下划线分词、首字母大写合并。 */
	static String tableNameToDomainName(String tableName) {
		String t = trimBackticks(StringUtils.trimToEmpty(tableName));
		if (t.isEmpty()) {
			return "Xxx";
		}
		String[] parts = StringUtils.split(t, "_");
		if (parts == null || parts.length == 0) {
			return StringUtils.capitalize(t);
		}
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (StringUtils.isNotBlank(p)) {
				sb.append(StringUtils.capitalize(p.toLowerCase()));
			}
		}
		if (sb.isEmpty()) {
			return StringUtils.capitalize(t);
		}
		return sb.toString();
	}

	private static String requestMappingFor(String domain) {
		return "/api/" + (Character.toLowerCase(domain.charAt(0)) + domain.substring(1));
	}

	private static List<Path> writeOneTable(Path javaRoot, String entityPackage, String mapperPackage, String appBase, String domain,
			String requestMapping) throws IOException {
		String voP = appBase + ".vo";
		String sp = appBase + ".service";
		String sip = appBase + ".service.impl";
		String wp = appBase + ".web";
		String entityF = entityPackage + "." + domain;
		String mapSimple = domain + "Mapper";
		String mapF = mapperPackage + "." + mapSimple;
		String voName = domain + "VO";
		String vof = voP + "." + voName;
		String sName = domain + "Service";
		String sif = sp + "." + sName;
		String implN = domain + "ServiceImpl";
		String cName = domain + "Controller";

		Path entityFile = javaRoot.resolve(entityPackage.replace('.', '/')).resolve(domain + ".java");
		if (!Files.isRegularFile(entityFile)) {
			throw new IllegalStateException("未找到实体源文件: " + entityFile
					+ "。请先执行 BaseMapperGeneratorUtil.generate，或调整 TableSpec.domainObjectName 与实体文件名。");
		}
		String entitySrc = Files.readString(entityFile, StandardCharsets.UTF_8);
		Set<String> voImports = voImportSetFromEntity(entitySrc);
		List<ParsedField> fields = parseEntityFields(entitySrc);
		String vo = buildVo(voP, voName, vof, entityF, fields, voImports);
		String si = buildServiceIfc(sp, sName, vof, voName);
		String sii = buildServiceImpl(sip, implN, sif, sName, mapSimple, mapF, entityF, voName, vof);
		String sc = buildController(wp, cName, vof, voName, sif, sName, requestMapping, domain);

		List<Path> out = new ArrayList<>(4);
		out.add(writeFile(javaRoot, voP, voName + ".java", vo));
		out.add(writeFile(javaRoot, sp, sName + ".java", si));
		out.add(writeFile(javaRoot, sip, implN + ".java", sii));
		out.add(writeFile(javaRoot, wp, cName + ".java", sc));
		return out;
	}

	/** 从实体复制 import，去掉表/ORM/Mapper 等，再补充 VO 所需。 */
	private static Set<String> voImportSetFromEntity(String entitySrc) {
		TreeSet<String> im = new TreeSet<>();
		for (String line : entitySrc.split("\r\n|\n")) {
			String t = line.trim();
			if (t.startsWith("import static ")) {
				continue;
			}
			if (!t.startsWith("import ")) {
				continue;
			}
			t = t.substring(7).replace(";", "").trim();
			if (shouldSkipEntityImport(t)) {
				continue;
			}
			im.add(t);
		}
		im.add("java.io.Serial");
		im.add("java.io.Serializable");
		im.add("io.swagger.v3.oas.annotations.media.Schema");
		im.add("lombok.Data");
		return im;
	}

	private static boolean shouldSkipEntityImport(String fqcn) {
		String s = fqcn;
		if (s.contains("mybatis")
				|| s.contains("apache.ibatis")
				|| s.contains("jakarta.persistence")
				|| s.contains("javax.persistence")
				|| s.contains("peach.common.mybatis.annotation")
				|| s.contains("peach.common.mybatis.mapper")
				|| s.contains("peach.common.mybatis.model")
				|| s.contains("peach.common.mybatis.annotation")) {
			return true;
		}
		if (s.endsWith("TableName")
				|| s.contains("org.apache.ibatis.annotations.Mapper")) {
			return true;
		}
		return s.endsWith("BaseMapper");
	}

	private static List<ParsedField> parseEntityFields(String src) {
		List<ParsedField> out = new ArrayList<>();
		for (String line : src.split("\r\n|\n")) {
			if (StringUtils.isBlank(line)) {
				continue;
			}
			if (line.contains(" static ")) {
				continue;
			}
			Matcher m = PRIVATE_FIELD.matcher(line);
			if (m.find()) {
				String typ = m.group(1).trim();
				String n = m.group(2);
				// 跳过 Lombok/序列化/复合注解下的重复行
				if ("serialVersionUID".equals(n)) {
					continue;
				}
				if (isKeywordLike(typ)) {
					continue;
				}
				out.add(new ParsedField(typ, n));
			}
		}
		return out;
	}

	private static boolean isKeywordLike(String typ) {
		return typ.isEmpty() || "class".equals(typ) || "if".equals(typ) || "return".equals(typ) || "public".equals(typ);
	}

	private static String buildVo(String voPackage, String voClass, String vof, String entityF, List<ParsedField> fields, Set<String> imports) {
		StringBuilder sb = new StringBuilder(512);
		sb.append("package ").append(voPackage).append(";\n\n");
		for (String imp : imports) {
			sb.append("import ").append(imp).append(";\n");
		}
		sb.append("\n/** 依据 {@link ");
		sb.append(entityF);
		sb.append("} 生成的对外 VO，不同步时手工改。\n */\n@Data\npublic class ").append(voClass).append(" implements Serializable {\n\n");
		sb.append("\t@Serial\n");
		sb.append("\tprivate static final long serialVersionUID = 1L;\n");
		if (fields.isEmpty()) {
			sb.append("\n");
		} else {
			for (ParsedField f : fields) {
				sb.append("\n");
				sb.append("\t@Schema(description = \"属性 ").append(f.name).append("\")\n");
				sb.append("\tprivate ").append(f.type).append(" ").append(f.name).append(";\n");
			}
		}
		sb.append("}\n");
		return sb.toString();
	}

	private static String buildServiceIfc(String sp, String sName, String vof, String voName) {
		return "package " + sp + ";\n\n" + "import " + vof + ";\n"
				+ "import org.peach.common.mybatis.service.BaseInterfaceService;\n\n"
				+ "/**\n * 业务服务接口：通用 CRUD 见 {@link BaseInterfaceService}，此处仅可追加扩展方法。\n */\n" + "public interface "
				+ sName + " extends BaseInterfaceService<" + voName + "> {\n}\n";
	}

	private static String buildServiceImpl(String sip, String implN, String sif, String sName, String mapSimple, String mapF, String entityF, String voName, String vof) {
		String eSimple = shortName(entityF);
		return "package " + sip + ";\n\n" + "import org.peach.common.mybatis.service.BaseAbstractService;\n"
				+ "import org.springframework.stereotype.Service;\n\n" + "import " + mapF + ";\n" + "import " + entityF
				+ ";\n" + "import " + vof + ";\n" + "import " + sif + ";\n\n" + "/**\n * 继承 {@link BaseAbstractService}。\n */\n"
				+ "@Service\n" + "public class " + implN + " extends BaseAbstractService<" + mapSimple + ", " + eSimple + ", "
				+ voName + "> implements " + sName + " {\n\n" + "\tpublic " + implN + "(" + mapSimple + " mapper) {\n"
				+ "\t\tsuper(mapper, " + eSimple + ".class, " + voName + ".class);\n" + "\t}\n" + "}\n";
	}

	private static String buildController(String wp, String cName, String vof, String voName, String sif, String sName, String requestMapping, String domain) {
		String implN = toServiceImplName(sName);
		String implF = wp.replace(".web", ".service.impl") + "." + implN;
		String s = "package " + wp + ";\n\n" + "import org.peach.common.mvc.web.BaseController;\n"
				+ "import org.springframework.web.bind.annotation.RequestMapping;\n"
				+ "import org.springframework.web.bind.annotation.RestController;\n\n" + "import " + vof + ";\n" + "import " + sif
				+ ";\n" + "import " + implF + ";\n\n" + "import io.swagger.v3.oas.annotations.tags.Tag;\n\n" + "/**\n * 继承 {@link BaseController}。\n */\n" + "@RestController\n" + "@RequestMapping(\""
				+ requestMapping + "\")\n" + "@Tag(name = \"" + domain + "接口\", description = \"依据代码生成，可改\")\n" + "public class "
				+ cName + " extends BaseController<" + voName + ", " + implN + "> {\n\n" + "\tpublic " + cName
				+ "(" + implN + " service) {\n" + "\t\tsuper(service);\n" + "\t}\n" + "}\n";
		return s;
	}

	private static String trimBackticks(String value) {
		String t = value;
		if (t.startsWith("`")) {
			t = t.substring(1);
		}
		if (t.endsWith("`")) {
			t = t.substring(0, t.length() - 1);
		}
		return t;
	}

	private static String toServiceImplName(String serviceInterfaceName) {
		if (serviceInterfaceName != null && serviceInterfaceName.endsWith("Service")) {
			return serviceInterfaceName.substring(0, serviceInterfaceName.length() - "Service".length())
					+ "ServiceImpl";
		}
		return serviceInterfaceName + "Impl";
	}

	private static String shortName(String fqn) {
		return fqn.substring(fqn.lastIndexOf('.') + 1);
	}

	private static Path writeFile(Path javaRoot, String pkg, String file, String text) throws IOException {
		Path p = javaRoot.resolve(pkg.replace('.', '/')).resolve(file);
		Files.createDirectories(p.getParent());
		Files.writeString(p, text.replace("\r\n", "\n"), StandardCharsets.UTF_8);
		return p;
	}

	private static final class ParsedField {
		final String type;
		final String name;

		ParsedField(String type, String name) {
			this.type = type;
			this.name = name;
		}
	}
}
