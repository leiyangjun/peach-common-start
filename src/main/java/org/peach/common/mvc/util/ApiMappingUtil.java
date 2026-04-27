package org.peach.common.mvc.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.SpringBeanUtil;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.swagger.v3.oas.annotations.Operation;

/**
 * 基于 Spring MVC {@link RequestMappingHandlerMapping} 的 API 元信息提取工具。
 * <p>
 * 适用场景：
 * </p>
 * <ul>
 * <li>动态 API 权限（菜单按钮 -> API 资源）</li>
 * <li>平台化任务配置（如 Quartz 定时调用 API）</li>
 * </ul>
 * <p>
 * 汇总规则：仅包含标注了 Swagger {@link Operation} 且 {@code summary} 非空的处理器；并排除 Actuator、常见静态资源路径模式。
 * </p>
 *
 * @author leiyangjun
 */
public final class ApiMappingUtil {

	/** 视为静态资源的路径模式后缀（PathPattern 串以小写比较）。 */
	private static final Set<String> STATIC_RESOURCE_SUFFIXES = Set.of(".html", ".htm", ".css", ".js", ".mjs", ".ico",
			".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".woff", ".woff2", ".ttf", ".eot", ".map");

	private ApiMappingUtil() {
	}

	/**
	 * 获取当前服务所有 API 映射信息（按 pathPattern + method 排序）。
	 */
	public static List<ApiMeta> listAllApis() {
		RequestMappingHandlerMapping mapping = SpringBeanUtil.getBean(RequestMappingHandlerMapping.class);
		List<ApiMeta> apis = new ArrayList<>();
		for (var entry : mapping.getHandlerMethods().entrySet()) {
			RequestMappingInfo info = entry.getKey();
			HandlerMethod handler = entry.getValue();
			OperationDoc op = extractOperationDoc(handler);
			if (op == null) {
				continue;
			}
			List<String> patterns = extractPatterns(info);
			Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
			if (patterns.isEmpty()) {
				continue;
			}
			String handlerFq = handler.getBeanType().getName() + "#" + handler.getMethod().getName();
			for (String pattern : patterns) {
				if (shouldExcludePathPattern(pattern)) {
					continue;
				}
				if (methods == null || methods.isEmpty()) {
					apis.add(buildMeta("ALL", pattern, op, handlerFq));
					continue;
				}
				for (RequestMethod method : methods) {
					apis.add(buildMeta(method.name(), pattern, op, handlerFq));
				}
			}
		}
		apis.sort(Comparator.comparing(ApiMeta::getPathPattern).thenComparing(ApiMeta::getMethod));
		return apis;
	}

	/**
	 * 按 Method 与关键字过滤 API。
	 *
	 * @param method    HTTP Method（GET/POST/...）
	 * @param keyword   模糊关键字（匹配 path 与接口说明，可空）
	 * @return 过滤后的 API 列表
	 */
	public static List<ApiMeta> searchApis(String method, String keyword) {
		String keywordNorm = normalize(keyword);
		String methodNorm = normalize(method);
		return listAllApis().stream().filter(api -> matchMethod(api, methodNorm)).filter(api -> matchKeyword(api, keywordNorm))
				.collect(Collectors.toList());
	}

	private static ApiMeta buildMeta(String httpMethod, String pattern, OperationDoc op, String handlerFq) {
		ApiMeta meta = new ApiMeta();
		meta.setMethod(httpMethod);
		meta.setPathPattern(pattern);
		meta.setUrlPath(pattern);
		meta.setSummary(op.summary());
		meta.setDescription(op.description());
		meta.setHandler(handlerFq);
		meta.setApiDesc(op.summary());
		return meta;
	}

	/**
	 * 无 {@link Operation} 或 {@code summary} 为空则返回 {@code null}，不参与汇总。
	 */
	private static OperationDoc extractOperationDoc(HandlerMethod handler) {
		Operation operation = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), Operation.class);
		if (operation == null) {
			return null;
		}
		String summary = StringUtils.trimToNull(operation.summary());
		if (summary == null) {
			return null;
		}
		return new OperationDoc(summary, StringUtils.trimToNull(operation.description()));
	}

	private record OperationDoc(String summary, String description) {
	}

	private static List<String> extractPatterns(RequestMappingInfo info) {
		PathPatternsRequestCondition pathPatterns = info.getPathPatternsCondition();
		if (pathPatterns == null || pathPatterns.getPatterns().isEmpty()) {
			return List.of();
		}
		return pathPatterns.getPatterns().stream().map(p -> p.getPatternString()).collect(Collectors.toList());
	}

	/**
	 * 排除 Actuator 与常见静态资源路径模式。
	 */
	private static boolean shouldExcludePathPattern(String pattern) {
		if (StringUtils.isBlank(pattern)) {
			return true;
		}
		String p = pattern.toLowerCase(Locale.ROOT);
		if (p.equals("/actuator") || p.startsWith("/actuator/")) {
			return true;
		}
		for (String suffix : STATIC_RESOURCE_SUFFIXES) {
			if (p.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchMethod(ApiMeta api, String methodNorm) {
		if (StringUtils.isBlank(methodNorm)) {
			return true;
		}
		String am = normalize(api.getMethod());
		if ("all".equals(am)) {
			return true;
		}
		return Objects.equals(methodNorm, am);
	}

	private static boolean matchKeyword(ApiMeta api, String keywordNorm) {
		if (StringUtils.isBlank(keywordNorm)) {
			return true;
		}
		return normalize(api.getUrlPath()).contains(keywordNorm) || normalize(api.getPathPattern()).contains(keywordNorm)
				|| normalize(api.getApiDesc()).contains(keywordNorm) || normalize(api.getSummary()).contains(keywordNorm)
				|| normalize(api.getDescription()).contains(keywordNorm) || normalize(api.getHandler()).contains(keywordNorm);
	}

	private static String normalize(String str) {
		return StringUtils.trimToEmpty(str).toLowerCase(Locale.ROOT);
	}
}
