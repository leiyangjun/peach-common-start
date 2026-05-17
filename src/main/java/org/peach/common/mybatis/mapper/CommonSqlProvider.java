package org.peach.common.mybatis.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mvc.exception.BizException;
import org.peach.common.mybatis.annotation.Exclude;
import org.peach.common.mybatis.annotation.ID;
import org.peach.common.mybatis.annotation.LogicDelete;
import org.peach.common.mybatis.annotation.Range;
import org.peach.common.mybatis.annotation.SearchValue;
import org.peach.common.mybatis.annotation.SqlTypeHandler;
import org.peach.common.mybatis.annotation.TableName;
import org.peach.common.mybatis.annotation.Unique;
import org.peach.common.mybatis.code.CrudBizCode;
import org.peach.common.mybatis.model.vo.RangeVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.utils.IdUtil;
import org.peach.common.utils.LoginUserUtil;
import org.springframework.util.CollectionUtils;

/**
 * 各 {@link InsertSqlProvider}、{@link DeleteSqlProvider}、{@link UpdateSqlProvider}、{@link SelectSqlProvider}
 * 共用的工具类：解析 {@link TableName}、{@link ID}、{@link LogicDelete} 等注解，生成列清单、主键、排序与区间条件等，
 * 供 MyBatis {@code Provider} 拼装动态 SQL；异常统一通过 {@link org.peach.common.mybatis.code.CrudBizCode} 抛出。
 */
public final class CommonSqlProvider {

	private CommonSqlProvider() {
	}

	private static final Pattern SAFE_FIELD_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");
	/**
	 * 实体字段元数据缓存：按 {@link Class} 首次解析后复用。
	 */
	private static final ConcurrentHashMap<Class<?>, Field[]> ALL_DECLARED_FIELDS_CACHE = new ConcurrentHashMap<>();
	/** 按类 + 属性名缓存 {@link FieldUtils#getField} 结果；值用 Optional 表示「曾解析但不存在」以防反复反射。 */
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Optional<Field>>> FIELD_BY_NAME_CACHE = new ConcurrentHashMap<>();
	private static final Set<String> CREATE_AUDIT_COLUMNS = Set.of("creator", "createTime", "creatorId", "creatorName");
	private static final Set<String> EXCLUDED_BASE_COLUMNS = Set.of("creator", "editor", "createTime", "editTime",
			"creatorId", "editorId", "creatorName", "editorName");

	/** 新增时由登录用户填充的四类审计字段（属性名与实体一致）。 */
	private static final Set<String> INSERT_AUDIT_COLUMNS = Set.of("creator", "editor", "createTime", "editTime");

	/**
	 * SQL 保留字（大写）：列名经 {@link #rename} 后与其中任一项相同（忽略大小写比较）时，在 PostgreSQL 等库中须用双引号包裹，
	 * 否则会被解析为关键字（例如列 {@code DESC} 在 {@code ORDER BY DESC DESC} 中会语法错误）。
	 */
	private static final Set<String> SQL_RESERVED_WORDS = Stream.of(
			"ALL", "AND", "ANY", "AS", "ASC", "BETWEEN", "BY", "CASE", "CAST", "CHECK", "COLUMN", "CONSTRAINT",
			"CREATE", "CROSS", "CURRENT", "DEFAULT", "DELETE", "DESC", "DISTINCT", "DO", "DROP", "ELSE", "END",
			"EXCEPT", "EXISTS", "FALSE", "FOR", "FOREIGN", "FROM", "FULL", "GRANT", "GROUP", "HAVING", "IN",
			"INDEX", "INNER", "INSERT", "INTERSECT", "INTO", "IS", "JOIN", "KEY", "LEFT", "LEVEL", "LIKE", "LIMIT",
			"NOT", "NULL", "OFFSET", "ON", "OR", "ORDER", "OUTER", "PRIMARY", "REFERENCES", "RIGHT", "ROLE", "SCHEMA",
			"SELECT", "SESSION", "SET", "SOME", "TABLE", "THEN", "TRUE", "TYPE", "UNION", "UNIQUE", "UPDATE", "USER",
			"USING", "VALUE", "VALUES", "VIEW", "WHEN", "WHERE", "WITH", "WITHOUT")
			.collect(Collectors.toUnmodifiableSet());

	/**
	 * 读取 JavaBean 实例字段；{@code bean} 为 {@link Class} 或 {@code null} 时返回 {@code null}。
	 * <p>
	 * 使用反射直读字段值。
	 * </p>
	 *
	 * @param bean 实体实例（勿传 {@link Class}）
	 * @param name 属性名（驼峰）
	 */
	public static Object readField(Object bean, String name) {
		if (bean == null || bean instanceof Class<?>) {
			return null;
		}
		try {
			return FieldUtils.readField(bean, name, true);
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	/**
	 * 是否视为「空」主键/SQL 条件值：{@code null}，或对 {@link CharSequence} 长度为 0。
	 */
	public static boolean isEmptyKeyValue(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof CharSequence) {
			return ((CharSequence) value).length() == 0;
		}
		return false;
	}

	/** 读取实体属性（实例），等同 {@link #readField}，供 Provider 内部简短调用。 */
	static Object prop(Object bean, String name) {
		return readField(bean, name);
	}

	/** 主键/字符串类条件是否已填写：非 null 且非空串。 */
	static boolean hasNonEmptyValue(Object bean, String name) {
		return !isEmptyKeyValue(prop(bean, name));
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	static boolean isCreateAuditColumn(String column) {
		return CREATE_AUDIT_COLUMNS.contains(column);
	}

	private static boolean containsIgnoreCaseText(String source, String part) {
		return lower(source).contains(lower(part));
	}

	private static String findColumnIgnoreCase(List<String> columns, String target) {
		String expected = lower(target);
		for (String column : columns) {
			if (expected.equals(lower(column))) {
				return column;
			}
		}
		return null;
	}

	/**
	 * UPDATE 场景追加<strong>修改人、修改时间</strong>；{@link LoginUserUtil#getLoginUserId()} 为 {@code null} 时不追加。
	 *
	 * @param owner 实体实例或实体 {@link Class}（用于解析字段 JDBC 类型）
	 */
	static void appendEditorAuditSet(SQL sql, List<String> columns, Object owner) {
		Long uid = LoginUserUtil.getLoginUserId();
		if (uid == null) {
			return;
		}
		Class<?> clazz = owner instanceof Class ? (Class<?>) owner : owner.getClass();
		String editTimeColumn = findColumnIgnoreCase(columns, "editTime");
		if (editTimeColumn != null) {
			sql.SET(sqlColumnName(editTimeColumn) + "=CURRENT_TIMESTAMP");
		}
		String editorIdColumn = findColumnIgnoreCase(columns, "editorId");
		String editorColumn = findColumnIgnoreCase(columns, "editor");
		String editorNameColumn = findColumnIgnoreCase(columns, "editorName");
		if (editorIdColumn != null) {
			sql.SET(sqlColumnName(editorIdColumn) + "=" + literalAuditUserValue(clazz, "editorId", uid));
		}
		if (editorColumn != null) {
			sql.SET(sqlColumnName(editorColumn) + "=" + literalAuditUserValue(clazz, "editor", uid));
		}
		String nickname = LoginUserUtil.getLoginUserNickname();
		if (editorNameColumn != null && StringUtils.isNotBlank(nickname)) {
			sql.SET(sqlColumnName(editorNameColumn) + "='" + escapeSqlString(nickname) + "'");
		}
	}

	static boolean isInsertAuditColumnName(String column) {
		return INSERT_AUDIT_COLUMNS.contains(column);
	}

	/** 修改侧审计列，由 {@link #appendEditorAuditSet} 统一写入，不参与实体 SET。 */
	static boolean isEditorAuditColumn(String column) {
		return "editor".equals(column) || "editTime".equals(column) || "editorId".equals(column)
				|| "editorName".equals(column);
	}

	/**
	 * 插入审计：{@code creator/editor} 用数值或字符串字面量；{@code createTime/editTime} 用 {@code CURRENT_TIMESTAMP}。
	 */
	static String literalAuditUserValue(Class<?> clazz, String property, Long uid) {
		Field f = getDeclaredField(clazz, property);
		if (f != null && CharSequence.class.isAssignableFrom(f.getType())) {
			return "'" + escapeSqlString(String.valueOf(uid)) + "'";
		}
		return String.valueOf(uid);
	}

	private static String escapeSqlString(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("'", "''");
	}

	/**
	 * INSERT 时若实体属性为空且存在登录用户，则生成审计列字面量（否则返回 {@code null} 表示走默认 #{prop} 或跳过）。
	 */
	static String insertAuditValueLiteral(Object obj, String column, Long uid) {
		if (uid == null || prop(obj, column) != null) {
			return null;
		}
		Class<?> clazz = obj.getClass();
		if ("createTime".equals(column) || "editTime".equals(column)) {
			return "CURRENT_TIMESTAMP";
		}
		if ("creator".equals(column) || "editor".equals(column)) {
			return literalAuditUserValue(clazz, column, uid);
		}
		return null;
	}

	/**
	 * 追加区间条件：实体中仅允许一个字段标记 {@link Range}，并使用 range 的 startValue/endValue。
	 * <p>
	 * {@link org.peach.common.mybatis.mapper.BaseMapper#likeSelectBase} 在 {@code range != null} 时由
	 * {@link SelectSqlProvider#likeSelectBaseSQL} 调用本方法；业务自定义 SQL 亦可直接复用。
	 * </p>
	 */
	static void appendRangeConditions(SQL sql, Object entity, RangeVO range) {
		if (range == null) {
			return;
		}
		List<Field> rangeFields = Arrays.stream(getDeclaredFields(entity)).filter(field -> field.getAnnotation(Range.class) != null)
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(rangeFields)) {
			return;
		}
		if (rangeFields.size() > 1) {//RANGE_FIELD_MULTIPLE
			throw BizException.validWarn(CrudBizCode.RANGE_FIELD_MULTIPLE, String.valueOf(rangeFields.size()));
		}
		String column = sqlColumnName(rangeFields.get(0).getName());
		if (range.getStartValue() != null) {
			sql.WHERE(column + " >= #{range.startValue}");
		}
		if (range.getEndValue() != null) {
			sql.WHERE(column + " <= #{range.endValue}");
		}
	}

	/**
	 * 解析逻辑表名：优先 {@link TableName#value()}，否则由类简单名驼峰转下划线大写（如 {@code User} →
	 * {@code USER}）。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 表名
	 */
	public static String getTableName(Object entityClass) {
		Class<?> clazz = entityClass instanceof Class ? (Class<?>) entityClass : entityClass.getClass();
		TableName tn = clazz.getAnnotation(TableName.class);
		if (tn != null && StringUtils.isNotBlank(tn.value())) {
			return tn.value();
		}
		return rename(clazz.getSimpleName());
	}

	/**
	 * 返回参与 SQL 的实体属性名列表（驼峰小写）；排除 {@link Exclude}、{@link Range} 等非表列字段，见
	 * {@link #getFilterAnnotationName}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 属性名列表
	 */
	public static List<String> getTableColumns(Object entityClass) {
		return getFilterAnnotationName(entityClass, null);
	}

	/**
	 * 在 {@link #getTableColumns(Object)} 基础上，若 {@code containBase} 为 {@code false}
	 * 则移除常见审计字段名（creator/editor/createtime 等）。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param containBase 为 {@code true} 时保留审计列
	 * @return 属性名列表
	 */
	public static List<String> getTableColumns(Object entityClass, boolean containBase) {
		List<String> columns = getTableColumns(entityClass);
		if (!containBase) {
			columns.removeIf(EXCLUDED_BASE_COLUMNS::contains);
		}
		return columns;
	}

	/**
	 * 将 {@link #getTableColumns(Object)} 得到的每个属性名经 {@link #rename} 转为库侧大写下划线形式，供
	 * SELECT 列清单使用。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 列名列表（大写下划线）
	 */
	public static List<String> getUpperTableColumns(Object entityClass) {
		return getFilterAnnotationName(entityClass, null).stream().map(CommonSqlProvider::sqlColumnName)
				.collect(Collectors.toList());
	}

	/**
	 * 解析单主键属性名：优先 {@link ID} 标注字段，否则默认 {@code id}；不支持复合主键，多处标注将抛错。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param rename      为 {@code true} 时返回 {@link #rename} 后的列名
	 * @return 主键字段名，无效时可能为空串
	 * @throws BizException 存在多个 {@link ID} 时
	 */
	public static String getKey(Object entityClass, boolean rename) {
		String tableKes = "id";
		List<String> fields = getFilterAnnotationName(entityClass, ID.class);
		if (!CollectionUtils.isEmpty(fields) && fields.size() > 1) {
			throw BizException.validWarn(CrudBizCode.ENTITY_ANNOTATION_CONFLICT);
		}
		tableKes = !CollectionUtils.isEmpty(fields) ? fields.get(0) : tableKes;
		tableKes = checkFieldExists(tableKes, entityClass) ? tableKes : "";
		return rename ? sqlColumnName(tableKes) : tableKes;
	}

	/**
	 * 解析逻辑删除属性名：优先 {@link LogicDelete} 字段，否则默认 {@code valid}；规则同 {@link #getKey} 的
	 * rename 与存在性校验。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param rename      为 {@code true} 时返回库列名形式
	 * @return 属性名或空串
	 * @throws BizException 多处 {@link LogicDelete} 时
	 */
	public static String getLogicDeleteField(Object entityClass, boolean rename) {
		String tableKes = "valid";
		List<String> fields = getFilterAnnotationName(entityClass, LogicDelete.class);
		if (!CollectionUtils.isEmpty(fields) && fields.size() > 1) {
			throw BizException.validWarn(CrudBizCode.ENTITY_ANNOTATION_CONFLICT);
		}
		tableKes = !CollectionUtils.isEmpty(fields) ? fields.get(0) : tableKes;
		tableKes = checkFieldExists(tableKes, entityClass) ? tableKes : "";
		return rename ? sqlColumnName(tableKes) : tableKes;
	}

	/**
	 * 判断某属性是否视为「表映射字段」：存在、非静态 final、非 {@link Exclude}，且类型为 JDK 基本包装等。
	 *
	 * @param field       属性名
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 是否可作为表列参与 SQL
	 */
	public static boolean checkFieldExists(String field, Object entityClass) {
		Field fieldTemp = getDeclaredField(entityClass, field);
		boolean result = true;
		if (fieldTemp == null || fieldTemp.getType().getClassLoader() != null
				|| !fieldTemp.getType().getName().startsWith("java") || Modifier.isStatic(fieldTemp.getModifiers())
				|| Modifier.isFinal(fieldTemp.getModifiers()) || fieldTemp.getAnnotation(Exclude.class) != null) {
			result = false;
		} /**
			 * else if () { result = false; } else if
			 * (!fieldTemp.getType().getName().startsWith("java")) { result = false; } else
			 * if (Modifier.isStatic(fieldTemp.getModifiers()) ||
			 * Modifier.isFinal(fieldTemp.getModifiers())) { result = false; } else if
			 * (fieldTemp.getAnnotation(Exclude.class) != null) { result = false; }
			 */
		return result;
	}

	/**
	 * 返回标注了 {@link LogicDelete} 的 {@link Field}，唯一；否则无注解时返回 {@code null}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 字段或 {@code null}
	 * @throws BizException 多处 {@link LogicDelete} 时
	 */
	public static Field getLogicDeleteField(Object entityClass) {
		List<Field> fields = getFilterAnnotation(entityClass, LogicDelete.class);
		if (!CollectionUtils.isEmpty(fields) && fields.size() > 1) {
			throw BizException.validWarn(CrudBizCode.ENTITY_ANNOTATION_CONFLICT);
		}
		return !CollectionUtils.isEmpty(fields) ? fields.get(0) : null;
	}

	/**
	 * 在生成 INSERT 语句<strong>之前</strong>，对「非库自增」且主键仍为空的实体<strong>反射写入</strong>应用侧主键。
	 * <p>
	 * 规则：{@link ID#isSequence()} 为 true 且 {@link ID#sequenceTag()} 为空时视为<strong>数据库自增</strong>，不修改实体；
	 * {@link ID#sequenceTag()} 非空时视为库侧序列表达式，仍不修改实体（由 SQL 字面量承担）；其余情况若主键为空，
	 * 则按 {@link ID#isSnowflakeHash()}、字段类型与 {@link IdUtil#shortSnowId()} 生成并 set 到实体，便于后续走
	 * {@code #{...}} 绑定且与 {@link #getTableKeyValue} 不再重复生成短雪花。
	 * </p>
	 *
	 * @param entity 表对应实体实例（不可为 Class 占位）
	 */
	public static void ensureApplicationGeneratedPrimaryKey(Object entity) {
		if (entity == null || entity instanceof Class) {
			return;
		}
		String pkProp = getKey(entity, false);
		if (StringUtils.isEmpty(pkProp)) {
			return;
		}
		Field f = getDeclaredField(entity, pkProp);
		if (f == null) {
			return;
		}
		ID idAn = f.getAnnotation(ID.class);
		if (idAn == null) {
			return;
		}
		if (idAn.isSequence() && StringUtils.isEmpty(idAn.sequenceTag())) {
			return;
		}
		if (idAn.isSequence() && StringUtils.isNotEmpty(idAn.sequenceTag())) {
			return;
		}
		if (hasNonEmptyValue(entity, pkProp)) {
			return;
		}
		try {
			f.setAccessible(true);
			Class<?> t = f.getType();
			if (idAn.isSnowflakeHash() || isIntegralNumericKeyType(t)) {
				assignIntegralPrimaryKey(f, entity, t, IdUtil.shortSnowId());
			} else if (isStringLikeKeyType(t)) {
				f.set(entity, IdUtil.shortId22());
			} else {
				f.set(entity, IdUtil.shortId22());
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("无法为 " + entity.getClass().getName() + " 写入主键", e);
		}
	}

	private static void assignIntegralPrimaryKey(Field f, Object entity, Class<?> t, long nid)
			throws IllegalAccessException {
		if (t == Long.class) {
			f.set(entity, Long.valueOf(nid));
		} else if (t == long.class) {
			f.setLong(entity, nid);
		} else if (t == Integer.class) {
			f.set(entity, Integer.valueOf((int) nid));
		} else if (t == int.class) {
			f.setInt(entity, (int) nid);
		} else if (t == Short.class) {
			f.set(entity, Short.valueOf((short) nid));
		} else if (t == short.class) {
			f.setShort(entity, (short) nid);
		} else if (t == Byte.class) {
			f.set(entity, Byte.valueOf((byte) nid));
		} else if (t == byte.class) {
			f.setByte(entity, (byte) nid);
		}
	}

	/**
	 * 主键缺省时的 SQL 片段：{@link String} 类用
	 * {@link IdUtil#shortId22()}；{@code long}/{@link Long} 用
	 * {@link IdUtil#shortSnowId()}； 自增/序列见
	 * {@link ID#isSequence()}、{@link ID#sequenceTag()}；显式
	 * {@link ID#isSnowflakeHash()} 强制短雪花。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param msgFormat   为批量插入 MessageFormat 转义预留的引号形式（仅影响字符串主键）
	 * @return 可直接拼进 SQL 的字面量或表达式字符串
	 */
	public static String getTableKeyValue(Object entityClass, boolean msgFormat) {
		String tableKeyStr = getKey(entityClass, false);
		Field tableKeyField = getDeclaredField(entityClass, tableKeyStr);
		ID idAn = tableKeyField == null ? null : tableKeyField.getAnnotation(ID.class);
		Class<?> ft = tableKeyField != null ? tableKeyField.getType() : String.class;

		if (tableKeyField != null && StringUtils.isNotEmpty(tableKeyStr) && hasNonEmptyValue(entityClass, tableKeyStr)) {
			return "";
		}

		if (idAn != null && idAn.isSequence() && StringUtils.isEmpty(idAn.sequenceTag())) {
			return "";
		}
		if (idAn != null && idAn.isSequence() && StringUtils.isNotEmpty(idAn.sequenceTag())) {
			return idAn.sequenceTag();
		}
		if (idAn != null && idAn.isSnowflakeHash()) {
			return formatKeyNumberSql(IdUtil.shortSnowId());
		}
		if (isIntegralNumericKeyType(ft)) {
			return formatKeyNumberSql(IdUtil.shortSnowId());
		}
		if (isStringLikeKeyType(ft)) {
			return formatKeyStringSql(IdUtil.shortId22(), msgFormat);
		}
		// 其它类型（如无 @ID）：与 String 相同采用 shortId22
		return formatKeyStringSql(IdUtil.shortId22(), msgFormat);
	}

	/** 整型数值主键（含 {@code long}/{@link Long}，及 int/Integer 等），空值时统一走短雪花。 */
	private static boolean isIntegralNumericKeyType(Class<?> t) {
		if (t == null) {
			return false;
		}
		return t == Long.class || t == long.class || t == Integer.class || t == int.class || t == Short.class
				|| t == short.class || t == Byte.class || t == byte.class;
	}

	private static boolean isStringLikeKeyType(Class<?> t) {
		return t != null && CharSequence.class.isAssignableFrom(t);
	}

	/** 数值主键 SQL 片段（不加引号）。 */
	private static String formatKeyNumberSql(long id) {
		return String.valueOf(id);
	}

	/** 字符串主键 SQL 字面量。 */
	private static String formatKeyStringSql(String id, boolean msgFormat) {
		return msgFormat ? "\'\'\'" + id + "\'\'\'" : "\'" + id + "\'";
	}

	/**
	 * 在全部声明字段中筛选：若指定 {@code filterAnnotation} 则只保留带该注解的；否则保留「可作为表列」的 Java 字段（含对
	 * {@link Range} 等的排除规则）。
	 *
	 * @param entityClass      实体实例或 {@link Class}
	 * @param filterAnnotation 限定注解类型，可为 {@code null} 表示不按单注解过滤
	 * @param <T>              注解类型
	 * @return 字段列表
	 */
	public static <T extends Annotation> List<Field> getFilterAnnotation(Object entityClass,
			Class<T> filterAnnotation) {
		List<Field> fieldList = Arrays.stream(getDeclaredFields(entityClass)).filter(field -> {// 过滤,只需要逻辑删除字段
			boolean result = true;
			if (filterAnnotation != null && field.getAnnotation(filterAnnotation) != null) {
				result = true;
			} else if (field.getType().getClassLoader() != null || !field.getType().getName().startsWith("java")
					|| Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
					|| field.getAnnotation(Exclude.class) != null || field.getAnnotation(Range.class) != null
					|| filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) {
				result = false;
			}
			/**
			 * else if (!field.getType().getName().startsWith("java")) { result = false; }
			 * else if (Modifier.isStatic(field.getModifiers()) ||
			 * Modifier.isFinal(field.getModifiers())) { result = false; } else if
			 * (field.getAnnotation(Exclude.class) != null) { result = false; } else if
			 * (filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) {
			 * result = false; }
			 */
			return result;
		}).collect(Collectors.toList());
		return fieldList;
	}

	/**
	 * 逻辑删除「有效」侧取值：字段名含 valid/delete 时按惯例推断，否则取 {@link LogicDelete#valid()}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 有效状态字面量
	 */
	public static String getLogicValidValue(Object entityClass) {
		String field = CommonSqlProvider.getLogicDeleteField(entityClass, false);
		String isDelete = containsIgnoreCaseText(field, "valid") ? "1" : "0";
		isDelete = containsIgnoreCaseText(field, "delete") ? "0" : isDelete;
		Field logicField = getDeclaredField(entityClass, field);
		if (!containsIgnoreCaseText(field, "valid") && !containsIgnoreCaseText(field, "delete") && logicField != null) {
			LogicDelete logicAn = logicField.getAnnotation(LogicDelete.class);
			isDelete = logicAn != null ? String.valueOf(logicAn.valid()) : isDelete;
		}
		return isDelete;
	}

	/**
	 * 逻辑删除「无效」侧取值：规则与 {@link #getLogicValidValue} 对称，否则取
	 * {@link LogicDelete#invalid()}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 无效状态字面量
	 */
	public static String getLogicInvalidValue(Object entityClass) {
		String field = CommonSqlProvider.getLogicDeleteField(entityClass, false);
		String isDelete = containsIgnoreCaseText(field, "valid") ? "0" : "1";
		isDelete = containsIgnoreCaseText(field, "delete") ? "1" : isDelete;
		Field logicField = getDeclaredField(entityClass, field);
		if (!containsIgnoreCaseText(field, "valid") && !containsIgnoreCaseText(field, "delete") && logicField != null) {
			LogicDelete logicAn = logicField.getAnnotation(LogicDelete.class);
			isDelete = logicAn != null ? String.valueOf(logicAn.invalid()) : isDelete;
		}
		return isDelete;
	}

	/**
	 * {@link #getFilterAnnotation} 的属性名列表形式。
	 *
	 * @param entityClass      实体实例或 {@link Class}
	 * @param filterAnnotation 限定注解，可为 {@code null}
	 * @param <T>              注解类型
	 * @return 属性名列表
	 */
	public static <T extends Annotation> List<String> getFilterAnnotationName(Object entityClass,
			Class<T> filterAnnotation) {
		List<String> fieldList = Arrays.stream(getDeclaredFields(entityClass)).filter(field -> {// 过滤,只需要逻辑删除字段
			boolean result = true;
			if (filterAnnotation != null && field.getAnnotation(filterAnnotation) != null) {
				result = true;
			} else if (field.getType().getClassLoader() != null || !field.getType().getName().startsWith("java")
					|| Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
					|| field.getAnnotation(Exclude.class) != null || field.getAnnotation(Range.class) != null
					|| filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) {
				result = false;
			}
			return result;
		}).map(Field::getName).collect(Collectors.toList());
		return fieldList;
	}

	/**
	 * 返回标有 {@link SearchValue} 的属性名，供模糊条件使用；库列名需再经 {@link #rename}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 属性名列表
	 */
	public static List<String> getSearchValue(Object entityClass) {
		return getFilterAnnotationName(entityClass, SearchValue.class);
	}

	/**
	 * 实体类及其父类上的全部 {@link Field}（含私有，与 MyBatis 反射习惯一致）。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 字段数组
	 */
	public static Field[] getDeclaredFields(Object entityClass) {
		Class<?> clazz = entityClass instanceof Class ? (Class<?>) entityClass : entityClass.getClass();
		return ALL_DECLARED_FIELDS_CACHE.computeIfAbsent(clazz, c -> {
			List<Field> all = FieldUtils.getAllFieldsList(c);
			return all.toArray(new Field[0]);
		});
	}

	/**
	 * 按名查找字段（向上继承链查找），不存在返回 {@code null}。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @param name        属性名
	 * @return 字段或 {@code null}
	 */
	public static Field getDeclaredField(Object entityClass, String name) {
		Class<?> clazz = entityClass instanceof Class ? (Class<?>) entityClass : entityClass.getClass();
		if (name == null) {
			return null;
		}
		ConcurrentHashMap<String, Optional<Field>> perClass = FIELD_BY_NAME_CACHE.computeIfAbsent(clazz,
				c -> new ConcurrentHashMap<>());
		return perClass.computeIfAbsent(name, n -> Optional.ofNullable(FieldUtils.getField(clazz, n, true))).orElse(null);
	}

	/**
	 * 返回标有 {@link Unique} 的属性名列表。
	 *
	 * @param entityClass 实体实例或 {@link Class}
	 * @return 属性名列表
	 */
	public static List<String> getUnique(Object entityClass) {
		return getFilterAnnotationName(entityClass, Unique.class);
	}

	/**
	 * 驼峰属性名 → 大写下划线列名（如 {@code abcEfg} → {@code ABC_EFG}）。
	 *
	 * @param name 属性名
	 * @return 列名
	 */
	public static String rename(String name) {
		return name.replaceAll("[A-Z]", " $0").trim().replaceAll(" ", "_").toUpperCase();
	}

	/**
	 * 若列名（通常已 {@link #rename}）为 {@link #SQL_RESERVED_WORDS} 中的保留字，则用双引号包裹以作为 PostgreSQL 标识符。
	 *
	 * @param columnName 库侧列名
	 * @return 原列名或 {@code "COLUMN"} 形式
	 */
	public static String quoteIfReserved(String columnName) {
		if (StringUtils.isBlank(columnName)) {
			return columnName;
		}
		if (SQL_RESERVED_WORDS.contains(columnName.toUpperCase(Locale.ROOT))) {
			return "\"" + columnName + "\"";
		}
		return columnName;
	}

	/**
	 * Java 驼峰属性名 → 库列名，并对 SQL 保留字加双引号；供 INSERT/UPDATE/SELECT/WHERE/ORDER BY 等列标识符位置统一使用。
	 *
	 * @param javaPropertyName 实体属性名（驼峰）
	 * @return 可安全写入 SQL 的列标识符
	 */
	public static String sqlColumnName(String javaPropertyName) {
		return quoteIfReserved(rename(javaPropertyName));
	}

	/**
	 * 下划线大写列名转驼峰属性名。
	 *
	 * @param name       如 {@code ABC_EFF}
	 * @param firstLower 为 {@code true} 时首字母小写（默认）
	 * @return 驼峰名
	 */
	public static String underlineToCamel(String name, Boolean firstLower) {
		if (StringUtils.isBlank(name)) {
			return name;
		}
		boolean firstLowerTemp = firstLower == null || firstLower;
		String normalized = StringUtils.lowerCase(name);
		StringBuilder sb = new StringBuilder(normalized.length());
		boolean upperNext = false;
		for (int i = 0; i < normalized.length(); i++) {
			char ch = normalized.charAt(i);
			if (ch == '_') {
				upperNext = true;
				continue;
			}
			if (upperNext) {
				sb.append(Character.toUpperCase(ch));
				upperNext = false;
			} else {
				sb.append(ch);
			}
		}
		if (!firstLowerTemp && sb.length() > 0) {
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		}
		return sb.toString();
	}

	/**
	 * {@link #underlineToCamel(String, Boolean)} 的首字母小写简写形式。
	 *
	 * @param name 列名
	 * @return 驼峰名
	 */
	public static String underlineToCamel(String name) {
		return underlineToCamel(name, true);
	}

	/**
	 * 与 {@link #rename} 等价，命名上与 {@link #underlineToCamel} 成对使用。
	 *
	 * @param name 属性名
	 * @return 大写下划线列名
	 */
	public static String camelToUnderLine(String name) {
		return name.replaceAll("[A-Z]", " $0").trim().replaceAll(" ", "_").toUpperCase();
	}

	/**
	 * 从查询对象上读取 {@code sortName}/{@code sortType} 生成 {@code ORDER BY} 片段；未指定时尝试
	 * {@code rowno} 升序或 {@code EditTime} 降序。
	 *
	 * @param entityClass 含排序字段的 DTO/实体
	 * @return 排序子句，可能为空串
	 */
	public static String orderBy(Object entityClass) {
		// descending ascending
		return orderBy(entityClass, "");
	}

	/**
	 * 仅从 {@link SortVO} 读取排序字段与方向；{@code sort} 为 {@code null} 或 {@code sortName}/{@code sortType}
	 * 任一方为空时<strong>不生成 ORDER BY</strong>（返回空串），不再从实体/DTO 上读取嵌入排序字段。
	 */
	public static String orderBy(Object entityClass, SortVO sort) {
		if (sort == null) {
			return "";
		}
		String sortName = sort.getSortName();
		String sortType = sort.getSortType();
		if (StringUtils.isNotBlank(sortName) && StringUtils.isNotBlank(sortType)) {
			return orderByFromExplicitSort(entityClass, StringUtils.trim(sortName), StringUtils.trim(sortType));
		}
		return "";
	}

	private static String orderByFromExplicitSort(Object entityClass, String sortName, String sortType) {
		if (!SAFE_FIELD_NAME.matcher(sortName).matches()) {
			throw BizException.validWarn(CrudBizCode.SORT_FIELD_PATTERN_INVALID, sortName);
		}
		List<String> columns = getTableColumns(entityClass, true);
		if (!columns.contains(sortName)) {
			throw BizException.validWarn(CrudBizCode.SORT_FIELD_NOT_MAPPED, sortName);
		}
		sortType = lower(sortType);
		sortType = (sortType.startsWith("asc") || "0".equals(sortType)) ? "asc" : sortType;
		sortType = (sortType.startsWith("desc") || "1".equals(sortType)) ? "desc" : sortType;
		if (!"asc".equals(sortType) && !"desc".equals(sortType)) {
			throw BizException.validWarn(CrudBizCode.SORT_TYPE_INVALID, sortType);
		}
		return sqlColumnName(sortName) + " " + sortType;
	}

	/**
	 * 同 {@link #orderBy(Object)}，可为排序列加表别名前缀（联表场景）。
	 *
	 * @param entityClass 含排序字段的 DTO/实体
	 * @param aliasTable  表别名，可为空串
	 * @return 排序子句，可能为空串
	 */
	public static String orderBy(Object entityClass, String aliasTable) {
		// descending ascending
		String orderBy = "";
		List<String> columns = getTableColumns(entityClass);
		/*
		 * 仅从「自身声明了 sortName/sortType」的查询 DTO 上读取嵌入排序；普通表实体（如 User）无此字段，
		 * 若与 SortVO 组合使用且 SortVO 未带排序，会经 orderBy(entity, sort) 回退到本方法，此时不得对实体
		 * 调用 readField(sortName)，否则会触发 FieldUtils「Cannot locate field sortName」。
		 */
		boolean embeddedSortSupported = !(entityClass instanceof Class<?>);
		if (embeddedSortSupported) {
			Class<?> beanClass = entityClass.getClass();
			embeddedSortSupported = FieldUtils.getField(beanClass, "sortName", true) != null
					&& FieldUtils.getField(beanClass, "sortType", true) != null;
		}
		if (embeddedSortSupported) {
			Object sortNameObj = readField(entityClass, "sortName");
			Object sortTypeObj = readField(entityClass, "sortType");
			String sortName = sortNameObj == null ? null : String.valueOf(sortNameObj);
			String sortType = sortTypeObj == null ? null : String.valueOf(sortTypeObj);
			if (!StringUtils.isEmpty(sortName) && !StringUtils.isEmpty(sortType)) {
				sortType = lower(sortType);
				sortType = sortType.startsWith("asc") ? "asc" : sortType;
				sortType = sortType.startsWith("desc") ? "desc" : sortType;
				sortType = "0".equals(sortType) ? "asc" : sortType;
				sortType = "1".equals(sortType) ? "desc" : sortType;
				orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + sqlColumnName(sortName) + " " + sortType;
			}
		}
		if (StringUtils.isEmpty(orderBy)) {// 默认按照修改时间或者行号排序
			// columns.rowno
			for (String column : columns) {// 如果没有传,有行号,按行号排序,没有行号按时间排序
				if ("rowno".equalsIgnoreCase(column)) {
					orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + sqlColumnName(column) + " asc";
					break;
				}
			}
			if (StringUtils.isEmpty(orderBy)) {
				for (String column : columns) {
					if ("EditTime".equalsIgnoreCase(column)) {
						orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + sqlColumnName(column) + " desc";
						break;
					}
				}
			}
		}
		return orderBy;
	}

	/**
	 * 单参数实体下的根属性占位符：{@code #{prop}} 或 {@code #{prop,typeHandler=...}}。
	 *
	 * @param entityBean       实体实例；若为 {@link Class} 则按该类解析字段上的 {@link SqlTypeHandler}
	 * @param javaPropertyName Java 属性名（驼峰）
	 */
	public static String mybatisRootParam(Object entityBean, String javaPropertyName) {
		Class<?> clazz = entityBean instanceof Class ? (Class<?>) entityBean : entityBean.getClass();
		return mybatisParamWithPath(clazz, javaPropertyName, javaPropertyName);
	}

	/**
	 * {@code @Param("entity")} 下的属性占位符：{@code #{entity.prop}} 或带 {@code typeHandler}。
	 *
	 * @param entityBean       实体实例或实体 {@link Class}
	 * @param javaPropertyName Java 属性名
	 */
	public static String mybatisEntityParam(Object entityBean, String javaPropertyName) {
		Class<?> clazz = entityBean instanceof Class ? (Class<?>) entityBean : entityBean.getClass();
		return mybatisParamWithPath(clazz, "entity." + javaPropertyName, javaPropertyName);
	}

	static String mybatisParamWithPath(Class<?> entityClass, String pathInBrace, String javaPropertyName) {
		Field f = getDeclaredField(entityClass, javaPropertyName);
		if (f != null) {
			SqlTypeHandler ann = f.getAnnotation(SqlTypeHandler.class);
			if (ann != null && ann.value() != null) {
				return "#{" + pathInBrace + ",typeHandler=" + ann.value().getName() + "}";
			}
		}
		return "#{" + pathInBrace + "}";
	}

	/**
	 * 批量 INSERT 单行 VALUES 中某一属性的 MessageFormat 片段（单引号按 MessageFormat 转义花括号）。
	 */
	public static String batchInsertListPropertySlot(Class<?> entityClass, String javaPropertyName) {
		Field f = getDeclaredField(entityClass, javaPropertyName);
		String body = "list[{0,number,#}]." + javaPropertyName;
		if (f != null) {
			SqlTypeHandler ann = f.getAnnotation(SqlTypeHandler.class);
			if (ann != null && ann.value() != null) {
				body = body + ",typeHandler=" + ann.value().getName();
			}
		}
		return "#'{" + body + "}";
	}

}
