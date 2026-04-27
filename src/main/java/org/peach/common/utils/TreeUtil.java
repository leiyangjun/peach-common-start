package org.peach.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 列表转树工具（菜单、省市区、部门等多级结构通用）。
 * <p>
 * 该工具不限定模型类型，调用方只需提供：
 * </p>
 * <ul>
 * <li>如何获取当前节点 id（idGetter）</li>
 * <li>如何获取父节点 id（pidGetter）</li>
 * <li>如何把子节点列表回写到节点对象（childrenSetter）</li>
 * </ul>
 *
 * @author leiyangjun
 */
public final class TreeUtil {

	private TreeUtil() {
	}

	/**
	 * 将平铺列表转换为树结构。
	 * <p>
	 * 根节点判定规则由 {@code rootPredicate} 决定；若某节点的父 id 在列表中不存在，也会被视为根节点。
	 * </p>
	 *
	 * @param <T>            节点类型
	 * @param <K>            id 类型
	 * @param source         原始平铺列表
	 * @param idGetter       节点 id 提取函数
	 * @param pidGetter      父节点 id 提取函数
	 * @param childrenSetter 子节点设置函数（如：Node::setChildren）
	 * @param rootPredicate  根节点判断函数（如：pid -> pid == null 或 pid == 0）
	 * @return 根节点列表（每个节点的 children 已回写）
	 */
	public static <T, K> List<T> toTree(List<T> source, Function<T, K> idGetter, Function<T, K> pidGetter,
			BiConsumer<T, List<T>> childrenSetter, Predicate<K> rootPredicate) {
		Objects.requireNonNull(idGetter, "idGetter");
		Objects.requireNonNull(pidGetter, "pidGetter");
		Objects.requireNonNull(childrenSetter, "childrenSetter");
		Objects.requireNonNull(rootPredicate, "rootPredicate");
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}

		// 维护插入顺序，保证树结果顺序与原列表尽量一致
		Map<K, T> nodeById = new LinkedHashMap<>(source.size());
		Map<K, List<T>> childrenByPid = new LinkedHashMap<>();

		for (T item : source) {
			if (item == null) {
				continue;
			}
			K id = idGetter.apply(item);
			nodeById.put(id, item);
			K pid = pidGetter.apply(item);
			childrenByPid.computeIfAbsent(pid, k -> new ArrayList<>()).add(item);
		}

		// 统一回写 children（无子节点时写空列表，避免前端判空）
		for (T node : nodeById.values()) {
			K id = idGetter.apply(node);
			List<T> children = childrenByPid.get(id);
			childrenSetter.accept(node, children == null ? new ArrayList<>() : new ArrayList<>(children));
		}

		List<T> roots = new ArrayList<>();
		for (T node : nodeById.values()) {
			K pid = pidGetter.apply(node);
			if (rootPredicate.test(pid) || !nodeById.containsKey(pid)) {
				roots.add(node);
			}
		}
		return roots;
	}

	/**
	 * 将平铺列表转换为树结构（按固定根 pid 判定根节点）。
	 * <p>
	 * 常见调用：根 pid 为 {@code null}、{@code 0}、{@code "0"} 等。
	 * </p>
	 */
	public static <T, K> List<T> toTree(List<T> source, Function<T, K> idGetter, Function<T, K> pidGetter,
			BiConsumer<T, List<T>> childrenSetter, K rootPid) {
		Predicate<K> rootJudge = pid -> Objects.equals(pid, rootPid);
		return toTree(source, idGetter, pidGetter, childrenSetter, rootJudge);
	}
}

