package org.peach.common.mybatis.generator;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Document;

/**
 * MyBatis Generator 插件：仅生成 Entity 与 Mapper 接口（Mapper 继承
 * {@code org.peach.common.mybatis.mapper.BaseMapper&lt;Entity&gt;}，无多余方法），
 * <strong>不生成任何 XML Mapper 文件</strong>。
 * <p>
 * Entity 侧补充 Swagger {@code @Schema} 与 peach 持久化约定注解（{@code @TableName}、
 * {@code @ID}、{@code @LogicDelete} 等）。
 * </p>
 * <p>
 * 配置示例（{@code generatorConfig.xml} 的 {@code context} 内，且<strong>不要</strong>配置
 * {@code sqlMapGenerator}）：
 * </p>
 *
 * <pre>
 * &lt;plugin type="org.peach.common.mybatis.generator.MybatisGeneratorCustomPlugin"&gt;
 *   &lt;property name="logicDeleteColumn" value="VALID"/&gt;
 *   &lt;property name="addMapperAnnotation" value="true"/&gt;
 *   &lt;property name="generateExample" value="false"/&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author leiyangjun
 */
public class MybatisGeneratorCustomPlugin extends PluginAdapter {

	private static final String DEFAULT_LOGIC_COL = "VALID";

	private static final String PROP_LOGIC_DELETE_COLUMN = "logicDeleteColumn";
	private static final String PROP_ADD_MAPPER = "addMapperAnnotation";
	private static final String PROP_GENERATE_EXAMPLE = "generateExample";

	private static final String FN_BASE_MAPPER = "org.peach.common.mybatis.mapper.BaseMapper";
	private static final String FN_TABLE_NAME = "org.peach.common.mybatis.annotation.TableName";
	private static final String FN_ID = "org.peach.common.mybatis.annotation.ID";
	private static final String FN_LOGIC_DELETE = "org.peach.common.mybatis.annotation.LogicDelete";
	private static final String FN_SCHEMA = "io.swagger.v3.oas.annotations.media.Schema";
	private static final String FN_MAPPER = "org.apache.ibatis.annotations.Mapper";
	private static final String FN_DATA = "lombok.Data";

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		FullyQualifiedJavaType serializable = new FullyQualifiedJavaType("java.io.Serializable");
		topLevelClass.addImportedType(serializable);
		topLevelClass.addSuperInterface(serializable);
		topLevelClass.addImportedType(new FullyQualifiedJavaType(FN_DATA));
		topLevelClass.addAnnotation("@Data");
		topLevelClass.addImportedType(new FullyQualifiedJavaType("java.io.Serial"));

		Field serialVersionUID = new Field("serialVersionUID", new FullyQualifiedJavaType("long"));
		serialVersionUID.setVisibility(JavaVisibility.PRIVATE);
		serialVersionUID.setStatic(true);
		serialVersionUID.setFinal(true);
		serialVersionUID.setInitializationString("1L");
		serialVersionUID.addAnnotation("@Serial");
		topLevelClass.getFields().add(0, serialVersionUID);

		FullyQualifiedJavaType tableNameAnn = new FullyQualifiedJavaType(FN_TABLE_NAME);
		topLevelClass.addImportedType(tableNameAnn);
		String tableSqlName = resolveTableName(introspectedTable);
		topLevelClass.addAnnotation("@TableName(\"" + escapeJavaString(tableSqlName) + "\")");

		String tableRemark = introspectedTable.getRemarks();
		if (StringUtils.isNotBlank(tableRemark)) {
			FullyQualifiedJavaType schemaAnn = new FullyQualifiedJavaType(FN_SCHEMA);
			topLevelClass.addImportedType(schemaAnn);
			topLevelClass.addAnnotation("@Schema(description = \"" + escapeJavaString(tableRemark.trim()) + "\")");
		}

		return true;
	}

	@Override
	public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, Plugin.ModelClassType modelClassType) {
		return false;
	}

	@Override
	public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, Plugin.ModelClassType modelClassType) {
		return false;
	}

	@Override
	public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, Plugin.ModelClassType modelClassType) {
		if (!Plugin.ModelClassType.BASE_RECORD.equals(modelClassType)) {
			return true;
		}

		FullyQualifiedJavaType schemaAnn = new FullyQualifiedJavaType(FN_SCHEMA);
		topLevelClass.addImportedType(schemaAnn);
		String colRemark = introspectedColumn.getRemarks();
		String desc = StringUtils.isNotBlank(colRemark) ? colRemark.trim() : field.getName();
		field.addAnnotation("@Schema(description = \"" + escapeJavaString(desc) + "\")");

		if (isSinglePrimaryKeyColumn(introspectedColumn, introspectedTable)) {
			topLevelClass.addImportedType(new FullyQualifiedJavaType(FN_ID));
			if (introspectedColumn.isIdentity()) {
				field.addAnnotation("@ID(isSequence = true)");
			} else {
				field.addAnnotation("@ID");
			}
		}

		String logicCol = getProperty(PROP_LOGIC_DELETE_COLUMN, DEFAULT_LOGIC_COL);
		if (introspectedColumn.getActualColumnName() != null
				&& introspectedColumn.getActualColumnName().equalsIgnoreCase(logicCol)) {
			topLevelClass.addImportedType(new FullyQualifiedJavaType(FN_LOGIC_DELETE));
			field.addAnnotation("@LogicDelete");
		}

		return true;
	}

	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		interfaze.getMethods().clear();
		interfaze.getImportedTypes().clear();

		interfaze.getSuperInterfaceTypes().clear();
		FullyQualifiedJavaType entityType = introspectedTable.getRules().calculateAllFieldsClass();
		FullyQualifiedJavaType baseMapper = new FullyQualifiedJavaType(FN_BASE_MAPPER);
		baseMapper.addTypeArgument(entityType);
		interfaze.addSuperInterface(baseMapper);

		interfaze.addImportedType(entityType);
		interfaze.addImportedType(new FullyQualifiedJavaType(FN_BASE_MAPPER));

		if (isTrue(getProperty(PROP_ADD_MAPPER, "true"), true)) {
			interfaze.addImportedType(new FullyQualifiedJavaType(FN_MAPPER));
			interfaze.addAnnotation("@Mapper");
		}

		return true;
	}

	@Override
	public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		return isTrue(properties.getProperty(PROP_GENERATE_EXAMPLE), false);
	}

	/**
	 * 始终不生成 XML Mapper，仅保留 Java Entity 与 Mapper 接口。
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		return false;
	}

	private String getProperty(String key, String defaultValue) {
		String v = properties.getProperty(key);
		return v == null ? defaultValue : v;
	}

	private static boolean isTrue(String raw, boolean defaultWhenBlank) {
		if (StringUtils.isBlank(raw)) {
			return defaultWhenBlank;
		}
		return Boolean.parseBoolean(raw.trim());
	}

	private static String resolveTableName(IntrospectedTable introspectedTable) {
		String name = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
		if (StringUtils.isNotBlank(name)) {
			return name;
		}
		return introspectedTable.getTableConfiguration().getTableName();
	}

	private static boolean isSinglePrimaryKeyColumn(IntrospectedColumn column, IntrospectedTable table) {
		List<IntrospectedColumn> pk = table.getPrimaryKeyColumns();
		if (pk == null || pk.size() != 1) {
			return false;
		}
		return pk.get(0).getActualColumnName().equalsIgnoreCase(column.getActualColumnName());
	}

	private static String escapeJavaString(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
	}
}

