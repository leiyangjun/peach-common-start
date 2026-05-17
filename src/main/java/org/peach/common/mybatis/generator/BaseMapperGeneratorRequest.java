package org.peach.common.mybatis.generator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * 程序化调用 MyBatis Generator 时的入参：数据库连接、输出目录与表清单等，
 * 无需编写 {@code generatorConfig.xml}。
 */
public final class BaseMapperGeneratorRequest {

	private final String jdbcUrl;
	private final String jdbcUsername;
	private final String jdbcPassword;
	private final String jdbcDriverClass;
	private final String entityPackage;
	private final String mapperPackage;
	private final String javaSourceRoot;
	private final List<TableSpec> tables;
	private final boolean overwrite;
	private final String logicDeleteColumn;
	private final boolean addMapperAnnotation;
	private final boolean generateExample;
	private final String contextId;
	private final String encoding;

	private BaseMapperGeneratorRequest(Builder builder) {
		this.jdbcUrl = builder.jdbcUrl;
		this.jdbcUsername = builder.jdbcUsername;
		this.jdbcPassword = builder.jdbcPassword != null ? builder.jdbcPassword : "";
		this.jdbcDriverClass = builder.jdbcDriverClass;
		this.entityPackage = builder.entityPackage;
		this.mapperPackage = builder.mapperPackage;
		this.javaSourceRoot = builder.javaSourceRoot;
		this.tables = Collections.unmodifiableList(new ArrayList<>(builder.tables));
		this.overwrite = builder.overwrite;
		this.logicDeleteColumn = builder.logicDeleteColumn;
		this.addMapperAnnotation = builder.addMapperAnnotation;
		this.generateExample = builder.generateExample;
		this.contextId = builder.contextId;
		this.encoding = builder.encoding;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public String getJdbcUsername() {
		return jdbcUsername;
	}

	public String getJdbcPassword() {
		return jdbcPassword;
	}

	public String getJdbcDriverClass() {
		return jdbcDriverClass;
	}

	public String getEntityPackage() {
		return entityPackage;
	}

	public String getMapperPackage() {
		return mapperPackage;
	}

	public String getJavaSourceRoot() {
		return javaSourceRoot;
	}

	public List<TableSpec> getTables() {
		return tables;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public String getLogicDeleteColumn() {
		return logicDeleteColumn;
	}

	public boolean isAddMapperAnnotation() {
		return addMapperAnnotation;
	}

	public boolean isGenerateExample() {
		return generateExample;
	}

	public String getContextId() {
		return contextId;
	}

	public String getEncoding() {
		return encoding;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 单表描述：库表名、可选实体类名与 catalog/schema。
	 */
	public static final class TableSpec {

		private final String tableName;
		private final String domainObjectName;
		private final String catalog;
		private final String schema;

		private TableSpec(String tableName, String domainObjectName, String catalog, String schema) {
			this.tableName = Objects.requireNonNull(tableName, "tableName").trim();
			this.domainObjectName = StringUtils.isBlank(domainObjectName) ? null : domainObjectName.trim();
			this.catalog = StringUtils.isBlank(catalog) ? null : catalog.trim();
			this.schema = StringUtils.isBlank(schema) ? null : schema.trim();
		}

		public static TableSpec of(String tableName) {
			return new TableSpec(tableName, null, null, null);
		}

		public static TableSpec of(String tableName, String domainObjectName) {
			return new TableSpec(tableName, domainObjectName, null, null);
		}

		public static TableSpec of(String catalog, String schema, String tableName, String domainObjectName) {
			return new TableSpec(tableName, domainObjectName, catalog, schema);
		}

		public String getTableName() {
			return tableName;
		}

		public String getDomainObjectName() {
			return domainObjectName;
		}

		public String getCatalog() {
			return catalog;
		}

		public String getSchema() {
			return schema;
		}
	}

	public static final class Builder {

		private String jdbcUrl;
		private String jdbcUsername;
		private String jdbcPassword = "";
		private String jdbcDriverClass;
		private String entityPackage;
		private String mapperPackage;
		private String javaSourceRoot;
		private final List<TableSpec> tables = new ArrayList<>();
		private boolean overwrite = true;
		private String logicDeleteColumn = "VALID";
		private boolean addMapperAnnotation = true;
		private boolean generateExample = false;
		private String contextId = "peach";
		private String encoding = "UTF-8";

		public Builder jdbcUrl(String jdbcUrl) {
			this.jdbcUrl = jdbcUrl;
			return this;
		}

		public Builder jdbcUsername(String jdbcUsername) {
			this.jdbcUsername = jdbcUsername;
			return this;
		}

		public Builder jdbcPassword(String jdbcPassword) {
			this.jdbcPassword = jdbcPassword;
			return this;
		}

		/**
		 * 可选；不填时根据 {@link BaseMapperGeneratorUtil#inferJdbcDriverClass(String, String)} 从 URL 推断。
		 */
		public Builder jdbcDriverClass(String jdbcDriverClass) {
			this.jdbcDriverClass = jdbcDriverClass;
			return this;
		}

		public Builder entityPackage(String entityPackage) {
			this.entityPackage = entityPackage;
			return this;
		}

		public Builder mapperPackage(String mapperPackage) {
			this.mapperPackage = mapperPackage;
			return this;
		}

		/**
		 * Java 源码根目录（通常为模块下的 {@code src/main/java}），建议使用绝对路径。
		 */
		public Builder javaSourceRoot(String javaSourceRoot) {
			this.javaSourceRoot = javaSourceRoot;
			return this;
		}

		/**
		 * 同上，使用 {@link Path} 传入。
		 */
		public Builder javaSourceRoot(Path javaSourceRoot) {
			this.javaSourceRoot = javaSourceRoot == null ? null : javaSourceRoot.toAbsolutePath().normalize().toString();
			return this;
		}

		public Builder addTable(TableSpec spec) {
			this.tables.add(Objects.requireNonNull(spec, "spec"));
			return this;
		}

		public Builder overwrite(boolean overwrite) {
			this.overwrite = overwrite;
			return this;
		}

		public Builder logicDeleteColumn(String logicDeleteColumn) {
			this.logicDeleteColumn = logicDeleteColumn;
			return this;
		}

		public Builder addMapperAnnotation(boolean addMapperAnnotation) {
			this.addMapperAnnotation = addMapperAnnotation;
			return this;
		}

		public Builder generateExample(boolean generateExample) {
			this.generateExample = generateExample;
			return this;
		}

		public Builder contextId(String contextId) {
			this.contextId = contextId;
			return this;
		}

		public Builder encoding(String encoding) {
			this.encoding = encoding;
			return this;
		}

		public BaseMapperGeneratorRequest build() {
			if (StringUtils.isBlank(jdbcUrl)) {
				throw new IllegalStateException("jdbcUrl 不能为空");
			}
			if (StringUtils.isBlank(jdbcUsername)) {
				throw new IllegalStateException("jdbcUsername 不能为空");
			}
			if (StringUtils.isBlank(entityPackage)) {
				throw new IllegalStateException("entityPackage 不能为空");
			}
			if (StringUtils.isBlank(mapperPackage)) {
				throw new IllegalStateException("mapperPackage 不能为空");
			}
			if (StringUtils.isBlank(javaSourceRoot)) {
				throw new IllegalStateException("javaSourceRoot 不能为空");
			}
			if (tables.isEmpty()) {
				throw new IllegalStateException("至少通过 addTable 指定一张数据库表");
			}
			return new BaseMapperGeneratorRequest(this);
		}
	}
}
