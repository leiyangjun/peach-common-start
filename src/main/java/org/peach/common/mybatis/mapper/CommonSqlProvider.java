package org.peach.common.mybatis.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.jdbc.SQL;
import org.peach.common.mybatis.annotation.Exclude;
import org.peach.common.mybatis.annotation.ID;
import org.peach.common.mybatis.annotation.LogicDelete;
import org.peach.common.mybatis.annotation.Range;
import org.peach.common.mybatis.annotation.SearchValue;
import org.peach.common.mybatis.annotation.TableName;
import org.peach.common.mybatis.annotation.Unique;
import org.peach.common.mybatis.code.CrudBizCode;
import org.peach.common.mybatis.model.vo.CommonQueryVO;
import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.mybatis.support.AuditBridge;
import org.peach.common.mybatis.support.BeanProperties;
import org.peach.common.utils.IdUtil;
import org.springframework.util.CollectionUtils;

/**
 * 各 {@link InsertSqlProvider}、{@link DeleteSqlProvider}、{@link UpdateSqlProvider}、{@link SelectSqlProvider}
 * 共用的工具类：解析 {@link TableName}、{@link ID}、{@link LogicDelete} 等注解，生成列清单、主键、排序与区间条件等，
 * 供 MyBatis {@code Provider} 拼装动态 SQL；异常统一通过 {@link org.peach.common.mybatis.code.CrudBizCode} 抛出。
 *
 * @author leiyangjun
 */
public final class CommonSqlProvider {

	private CommonSqlProvider() {
	}

	private static final Pattern SAFE_FIELD_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");
	/**
	 * 实体类字段元数据缓存：同一 {@link Class} 在运行时不变，首次解析后复用，避免重复遍历继承链。
	 * （基于类对象身份；热部署/多 ClassLoader 场景下以类加载器区分的类型视为不同键。）
	 */
	private static final ConcurrentHashMap<Class<?>, Field[]> ALL_DECLARED_FIELDS_CACHE = new ConcurrentHashMap<>();
	/** 按类 + 属性名缓存 {@link FieldUtils#getField} 结果；值用 Optional 表示「曾解析但不存在」以防反复反射。 */
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Optional<Field>>> FIELD_BY_NAME_CACHE = new ConcurrentHashMap<>();
	private static final Set<String> CREATE_AUDIT_COLUMNS = Set.of("creator", "createTime", "creatorId", "creatorName");
	private static final Set<String> EXCLUDED_BASE_COLUMNS = Set.of("creator", "editor", "createTime", "editTime",
			"creatorId", "editorId", "creatorName", "editorName");

	/** 读取实体属性（实例），避免 JSON 往返序列化。 */
	static Object prop(Object bean, String name) {
		return BeanProperties.read(bean, name);
	}

	/** 主键/字符串类条件是否已填写：非 null 且非空串。 */
	static boolean hasNonEmptyValue(Object bean, String name) {
		return !BeanProperties.isEmptyKey(prop(bean, name));
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

	static void appendEditorAuditSet(SQL sql, List<String> columns) {
		String editTimeColumn = findColumnIgnoreCase(columns, "editTime");
		if (editTimeColumn != null) {
			sql.SET(rename(editTimeColumn) + "=CURRENT_TIMESTAMP");
		}
		String editorIdColumn = findColumnIgnoreCase(columns, "editorId");
		String editorColumn = findColumnIgnoreCase(columns, "editor");
		String editorNameColumn = findColumnIgnoreCase(columns, "editorName");
		if (editorIdColumn != null && StringUtils.isNotBlank(AuditBridge.getUserId())) {
			sql.SET(rename(editorIdColumn) + "='" + AuditBridge.getUserId() + "'");
		}
		if (editorColumn != null && StringUtils.isNotBlank(AuditBridge.getUserId())) {
			sql.SET(rename(editorColumn) + "='" + AuditBridge.getUserId() + "'");
		}
		if (editorNameColumn != null && StringUtils.isNotBlank(AuditBridge.getUserName())) {
			sql.SET(rename(editorNameColumn) + "='" + AuditBridge.getUserName() + "'");
		}
	}

	/**
	 * 追加区间条件：实体中仅允许一个字段标记 {@link Range}，并使用 query 的 startValue/endValue。
	 */
	static void appendRangeConditions(SQL sql, Object entity, CommonQueryVO query) {
		if (query == null) {
			return;
		}
		List<Field> rangeFields = Arrays.stream(getDeclaredFields(entity)).filter(field -> field.getAnnotation(Range.class) != null)
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(rangeFields)) {
			return;
		}
		if (rangeFields.size() > 1) {
			throw CrudBizCode.RANGE_FIELD_MULTIPLE.badRequestFormatted(rangeFields.size());
		}
		String column = rename(rangeFields.get(0).getName());
		if (query.getStartValue() != null) {
			sql.WHERE(column + " >= #{query.startValue}");
		}
		if (query.getEndValue() != null) {
			sql.WHERE(column + " <= #{query.endValue}");
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
		return getFilterAnnotationName(entityClass, null).stream().map(field -> rename(field))
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
			throw CrudBizCode.ENTITY_ANNOTATION_CONFLICT.badRequest();
		}
		tableKes = !CollectionUtils.isEmpty(fields) ? fields.get(0) : tableKes;
		tableKes = checkFieldExists(tableKes, entityClass) ? tableKes : "";
		return rename ? rename(tableKes) : tableKes;
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
			throw CrudBizCode.ENTITY_ANNOTATION_CONFLICT.badRequest();
		}
		tableKes = !CollectionUtils.isEmpty(fields) ? fields.get(0) : tableKes;
		tableKes = checkFieldExists(tableKes, entityClass) ? tableKes : "";
		return rename ? rename(tableKes) : tableKes;
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
			throw CrudBizCode.ENTITY_ANNOTATION_CONFLICT.badRequest();
		}
		return !CollectionUtils.isEmpty(fields) ? fields.get(0) : null;
	}

	/**
	 * 主键缺省时的 SQL 片段：{@link String} 类用
	 * {@link IdUtil#shortId22()}；{@code long}/{@link Long} 用
	 * {@link IdUtil#nextId()}； 自增/序列见
	 * {@link ID#isSequence()}、{@link ID#sequenceTag()}；显式
	 * {@link ID#isSnowflakeHash()} 强制雪花。
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

		if (idAn != null && idAn.isSequence() && StringUtils.isEmpty(idAn.sequenceTag())) {
			return "";
		}
		if (idAn != null && idAn.isSequence() && StringUtils.isNotEmpty(idAn.sequenceTag())) {
			return idAn.sequenceTag();
		}
		if (idAn != null && idAn.isSnowflakeHash()) {
			return formatKeyNumberSql(IdUtil.nextId());
		}
		if (isIntegralNumericKeyType(ft)) {
			return formatKeyNumberSql(IdUtil.nextId());
		}
		if (isStringLikeKeyType(ft)) {
			return formatKeyStringSql(IdUtil.shortId22(), msgFormat);
		}
		// 其它类型（如无 @ID）：与 String 相同采用短 ID，避免再依赖 32 位 UUID
		return formatKeyStringSql(IdUtil.shortId22(), msgFormat);
	}

	/** 整型数值主键（含 {@code long}/{@link Long}，及 int/Integer 等），空值时统一走雪花。 */
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
	 * 从 {@link SortVO} 读取排序；与 {@link #selectBaseSQL} 列策略一致时使用
	 * {@code getTableColumns(entity, true)}。
	 */
	public static String orderBy(Object entityClass, SortVO sort) {
		if (sort == null) {
			return orderBy(entityClass, "");
		}
		String sortName = sort.getSortName();
		String sortType = sort.getSortType();
		if (StringUtils.isNotBlank(sortName) && StringUtils.isNotBlank(sortType)) {
			return orderByFromExplicitSort(entityClass, StringUtils.trim(sortName), StringUtils.trim(sortType));
		}
		return orderBy(entityClass, "");
	}

	private static String orderByFromExplicitSort(Object entityClass, String sortName, String sortType) {
		if (!SAFE_FIELD_NAME.matcher(sortName).matches()) {
			throw CrudBizCode.SORT_FIELD_PATTERN_INVALID.badRequestFormatted(sortName);
		}
		List<String> columns = getTableColumns(entityClass, true);
		if (!columns.contains(sortName)) {
			throw CrudBizCode.SORT_FIELD_NOT_MAPPED.badRequestFormatted(sortName);
		}
		sortType = lower(sortType);
		sortType = (sortType.startsWith("asc") || "0".equals(sortType)) ? "asc" : sortType;
		sortType = (sortType.startsWith("desc") || "1".equals(sortType)) ? "desc" : sortType;
		if (!"asc".equals(sortType) && !"desc".equals(sortType)) {
			throw CrudBizCode.SORT_TYPE_INVALID.badRequestFormatted(sortType);
		}
		return rename(sortName) + " " + sortType;
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
		Object sortNameObj = BeanProperties.read(entityClass, "sortName");
		Object sortTypeObj = BeanProperties.read(entityClass, "sortType");
		String sortName = sortNameObj == null ? null : String.valueOf(sortNameObj);
		String sortType = sortTypeObj == null ? null : String.valueOf(sortTypeObj);
		if (!StringUtils.isEmpty(sortName) && !StringUtils.isEmpty(sortType)) {
			sortType = lower(sortType);
			sortType = sortType.startsWith("asc") ? "asc" : sortType;
			sortType = sortType.startsWith("desc") ? "desc" : sortType;
			sortType = "0".equals(sortType) ? "asc" : sortType;
			sortType = "1".equals(sortType) ? "desc" : sortType;
			orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + rename(sortName) + " " + sortType;
		} else {// 默认按照修改时间或者行号排序
			// columns.rowno
			for (String column : columns) {// 如果没有传,有行号,按行号排序,没有行号按时间排序
				if ("rowno".equalsIgnoreCase(column)) {
					orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + rename(column) + " asc";
					break;
				}
			}
			if (StringUtils.isEmpty(orderBy)) {
				for (String column : columns) {
					if ("EditTime".equalsIgnoreCase(column)) {
						orderBy = (StringUtils.isEmpty(aliasTable) ? "" : aliasTable + ".") + rename(column) + " desc";
						break;
					}
				}
			}
		}
		return orderBy;
	}

}
