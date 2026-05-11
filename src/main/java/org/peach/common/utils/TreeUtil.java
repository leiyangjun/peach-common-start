package org.peach.common.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.peach.common.utils.annotation.TreeId;
import org.peach.common.utils.annotation.TreeParentId;
import org.peach.common.utils.annotation.TreeSortField;

/**
 * 平铺列表转树：仅根据 VO 上的注解反射取值，无函数式参数。
 * <p>
 * 必填：{@link TreeId}、{@link TreeParentId}，以及 {@code List} 子节点字段（默认字段名
 * {@code children}）。<br>
 * 可选：{@link TreeSortField}；若未标注则<strong>不排序</strong>。
 * </p>
 */
public final class TreeUtil {

	private static final ConcurrentHashMap<Class<?>, TreeMeta> META_CACHE = new ConcurrentHashMap<>();

	private TreeUtil() {
	}

	/**
	 * 将 {@code source} 组装为树并返回根节点列表。
	 *
	 * @param source 平铺数据，可为 null 或空
	 * @param clazz  参与组树的类型（须含注解与 children）
	 */
	public static <T> List<T> tree(List<T> source, Class<T> clazz) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}
		TreeMeta meta = META_CACHE.computeIfAbsent(clazz, TreeMeta::parse);

		Map<Object, T> nodeById = new LinkedHashMap<>(source.size());
		Map<Object, List<T>> childrenByPid = new LinkedHashMap<>();

		for (int i = 0; i < source.size(); i++) {
			T item = source.get(i);
			if (item == null) {
				continue;
			}
			Object id = meta.getId(item);
			Object pid = meta.getParentId(item);
			nodeById.put(id, item);
			List<T> bucket = childrenByPid.get(pid);
			if (bucket == null) {
				bucket = new ArrayList<>();
				childrenByPid.put(pid, bucket);
			}
			bucket.add(item);
		}

		for (T node : nodeById.values()) {
			Object id = meta.getId(node);
			List<T> ch = childrenByPid.get(id);
			List<T> assign = ch == null ? new ArrayList<>() : new ArrayList<>(ch);
			meta.setChildren(node, assign);
		}

		List<T> roots = new ArrayList<>();
		for (T node : nodeById.values()) {
			Object pid = meta.getParentId(node);
			if (meta.isRoot(pid, nodeById)) {
				roots.add(node);
			}
		}

		if (meta.hasSortField()) {
			sortTreeRecursive(roots, meta);
		}
		return roots;
	}

	private static <T> void sortTreeRecursive(List<T> nodes, TreeMeta meta) {
		if (nodes == null || nodes.isEmpty()) {
			return;
		}
		Collections.sort(nodes, new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
				return meta.compareSortKeys(a, b);
			}
		});
		for (int i = 0; i < nodes.size(); i++) {
			T n = nodes.get(i);
			List<T> children = meta.getChildren(n);
			if (children != null && !children.isEmpty()) {
				sortTreeRecursive(children, meta);
			}
		}
	}

	private static final class TreeMeta {

		private final Class<?> beanClass;
		private final Method idGetter;
		private final Method pidGetter;
		private final Method childrenGetter;
		private final Method childrenSetter;
		private final Method sortGetter;
		private final Class<?> pidFieldType;

		private TreeMeta(Class<?> beanClass, Method idGetter, Method pidGetter, Method childrenGetter,
				Method childrenSetter, Method sortGetter, Class<?> pidFieldType) {
			this.beanClass = beanClass;
			this.idGetter = idGetter;
			this.pidGetter = pidGetter;
			this.childrenGetter = childrenGetter;
			this.childrenSetter = childrenSetter;
			this.sortGetter = sortGetter;
			this.pidFieldType = pidFieldType;
		}

		boolean hasSortField() {
			return sortGetter != null;
		}

		Object getId(Object bean) {
			return invoke(idGetter, bean);
		}

		Object getParentId(Object bean) {
			return invoke(pidGetter, bean);
		}

		void setChildren(Object bean, List<?> children) {
			try {
				childrenSetter.invoke(bean, children);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("setChildren 失败: " + beanClass.getName(), e);
			}
		}

		@SuppressWarnings("unchecked")
		<T> List<T> getChildren(Object bean) {
			try {
				return (List<T>) childrenGetter.invoke(bean);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("getChildren 失败: " + beanClass.getName(), e);
			}
		}

		int compareSortKeys(Object a, Object b) {
			Object va = invoke(sortGetter, a);
			Object vb = invoke(sortGetter, b);
			if (va == null && vb == null) {
				return 0;
			}
			if (va == null) {
				return 1;
			}
			if (vb == null) {
				return -1;
			}
			@SuppressWarnings({ "rawtypes", "unchecked" })
			int r = ((Comparable) va).compareTo(vb);
			return r;
		}

		boolean isRoot(Object pid, Map<Object, ?> nodeById) {
			if (pid == null) {
				return true;
			}
			if (!nodeById.containsKey(pid)) {
				return true;
			}
			Class<?> p = wrapPrimitive(pidFieldType);
			if (p == Long.class) {
				return Objects.equals(pid, 0L);
			}
			if (p == Integer.class) {
				return Objects.equals(pid, 0);
			}
			if (p == Short.class) {
				return Objects.equals(pid, Short.valueOf((short) 0));
			}
			if (p == String.class) {
				return "0".equals(pid);
			}
			return false;
		}

		private static Object invoke(Method m, Object target) {
			try {
				return m.invoke(target);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("调用 " + m.getName() + " 失败", e);
			}
		}

		private static TreeMeta parse(Class<?> clazz) {
			Field idField = requireAnnotatedField(clazz, TreeId.class);
			Field pidField = requireAnnotatedField(clazz, TreeParentId.class);
			Field sortField = optionalAnnotatedField(clazz, TreeSortField.class);
			Field childrenField = findChildrenListField(clazz);

			Method idGet = getterMethod(clazz, idField);
			Method pidGet = getterMethod(clazz, pidField);
			Method chGet = getterMethod(clazz, childrenField);
			Method chSet = setterMethod(clazz, childrenField);

			Method sortGet = null;
			if (sortField != null) {
				validateSortField(clazz, sortField);
				sortGet = getterMethod(clazz, sortField);
			}

			return new TreeMeta(clazz, idGet, pidGet, chGet, chSet, sortGet, pidField.getType());
		}

		private static void validateSortField(Class<?> clazz, Field sortField) {
			Class<?> t = sortField.getType();
			if (t.isPrimitive()) {
				throw new IllegalArgumentException(
						clazz.getName() + " @" + TreeSortField.class.getSimpleName() + " 请使用包装类型并实现 Comparable");
			}
			if (!Comparable.class.isAssignableFrom(t)) {
				throw new IllegalArgumentException(
						clazz.getName() + " @" + TreeSortField.class.getSimpleName() + " 须实现 Comparable");
			}
		}

		private static Field requireAnnotatedField(Class<?> clazz, Class<? extends Annotation> ann) {
			Field f = optionalAnnotatedField(clazz, ann);
			if (f == null) {
				throw new IllegalArgumentException(clazz.getName() + " 缺少 @" + ann.getSimpleName() + " 字段");
			}
			return f;
		}

		private static Field optionalAnnotatedField(Class<?> clazz, Class<? extends Annotation> ann) {
			Field found = null;
			for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
				for (Field fi : c.getDeclaredFields()) {
					if (fi.isAnnotationPresent(ann)) {
						if (found != null) {
							throw new IllegalArgumentException(
									clazz.getName() + " 存在多个 @" + ann.getSimpleName() + " 字段");
						}
						found = fi;
					}
				}
			}
			return found;
		}

		private static Field findChildrenListField(Class<?> clazz) {
			Field byName = null;
			Field byGeneric = null;
			for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
				for (Field f : c.getDeclaredFields()) {
					if (!List.class.isAssignableFrom(f.getType())) {
						continue;
					}
					Type gt = f.getGenericType();
					if (gt instanceof ParameterizedType pt) {
						Type a0 = pt.getActualTypeArguments()[0];
						if (a0.equals(clazz) || (a0 instanceof Class<?> ac && clazz.isAssignableFrom(ac))) {
							byGeneric = f;
						}
					}
					if ("children".equals(f.getName())) {
						byName = f;
					}
				}
			}
			if (byName != null) {
				return byName;
			}
			if (byGeneric != null) {
				return byGeneric;
			}
			throw new IllegalArgumentException(clazz.getName() + " 需声明 List 子节点字段（推荐 children）");
		}

		private static Method getterMethod(Class<?> clazz, Field field) {
			try {
				Class<?> ft = field.getType();
				String cap = capitalize(field.getName());
				if (ft == boolean.class || ft == Boolean.class) {
					try {
						return clazz.getMethod("is" + cap);
					} catch (NoSuchMethodException ignored) {
						// fall through
					}
				}
				return clazz.getMethod("get" + cap);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(clazz.getName() + " 字段 " + field.getName() + " 缺少 getter", e);
			}
		}

		private static Method setterMethod(Class<?> clazz, Field field) {
			try {
				return clazz.getMethod("set" + capitalize(field.getName()), List.class);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(clazz.getName() + " 字段 " + field.getName() + " 缺少 set(List)", e);
			}
		}

		private static String capitalize(String s) {
			if (s == null || s.isEmpty()) {
				return s;
			}
			return Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}

		private static Class<?> wrapPrimitive(Class<?> t) {
			if (!t.isPrimitive()) {
				return t;
			}
			if (t == long.class) {
				return Long.class;
			}
			if (t == int.class) {
				return Integer.class;
			}
			if (t == short.class) {
				return Short.class;
			}
			if (t == byte.class) {
				return Byte.class;
			}
			return t;
		}
	}
}
