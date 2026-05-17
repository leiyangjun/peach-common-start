package org.peach.common.mvc.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.SpringBeanUtil;
import org.peach.common.mvc.api.context.ApiType;
import org.peach.common.mvc.api.context.validator.ApiContextValidator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
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
 */
public final class ApiMappingUtil {

	/** 视为静态资源的路径模式后缀（PathPattern 串以小写比较）。 */
	private static final Set<String> STATIC_RESOURCE_SUFFIXES = Set.of(".html", ".htm", ".css", ".js", ".mjs", ".ico",
			".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".woff", ".woff2", ".ttf", ".eot", ".map");

	private ApiMappingUtil() {
	}

	/**
	 * 获取当前服务由 Spring MVC 扫描到的 API 映射（按 pathPattern + method 排序）。
	 */
	public static List<ApiMeta> listAllApis() {
		List<ApiMeta> apis = new ArrayList<>(scanMvcApis());
		apis.sort(Comparator.comparing(ApiMeta::getPathPattern).thenComparing(ApiMeta::getMethod));
		return apis;
	}

	/**
	 * 中央注册表：与 {@link #listAllApis()} 同源，供 {@code /apis} 与按 {@code apiType} 过滤的接口统一消费。
	 */
	public static List<ApiMeta> listApiRegistry() {
		return listAllApis();
	}

	private static List<ApiMeta> scanMvcApis() {
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
			for (String pattern : patterns) {
				if (shouldExcludePathPattern(pattern)) {
					continue;
				}
				if (methods == null || methods.isEmpty()) {
					apis.add(buildMeta("ALL", pattern, op, handler));
					continue;
				}
				for (RequestMethod method : methods) {
					apis.add(buildMeta(method.name(), pattern, op, handler));
				}
			}
		}
		return apis;
	}

	private static String resolveSpringApplicationName() {
		try {
			Environment env = SpringBeanUtil.getApplicationContext().getEnvironment();
			String name = StringUtils.trimToNull(env.getProperty("spring.application.name"));
			return name != null ? name : "application";
		}
		catch (IllegalStateException ex) {
			return "application";
		}
	}

	/**
	 * 按 Method 与关键字过滤 API。
	 *
	 * @param method    HTTP Method（GET/POST/...）
	 * @param keyword   模糊关键字（匹配路径、摘要、说明、服务名、apiType 等，可空）
	 * @param apiType   形态过滤：admin / app / openapi（大小写不敏感，可空表示不过滤）
	 * @return 过滤后的 API 列表
	 */
	public static List<ApiMeta> searchApis(String method, String keyword, String apiType) {
		String keywordNorm = normalize(decodeKeywordIfPercentEncodedLiteral(keyword));
		String methodNorm = normalize(method);
		String apiTypeNorm = normalize(apiType);
		return listApiRegistry().stream().filter(api -> matchMethod(api, methodNorm)).filter(api -> matchKeyword(api, keywordNorm))
				.filter(api -> matchApiType(api, apiTypeNorm)).collect(Collectors.toList());
	}

	/**
	 * 同 {@link #searchApis(String, String, String)}，不按形态过滤。
	 */
	public static List<ApiMeta> searchApis(String method, String keyword) {
		return searchApis(method, keyword, null);
	}

	private static ApiMeta buildMeta(String httpMethod, String pattern, OperationDoc op, HandlerMethod handler) {
		ApiMeta meta = new ApiMeta();
		meta.setMethod(httpMethod);
		meta.setPathPattern(pattern);
		meta.setUrlPath(pattern);
		meta.setSummary(op.summary());
		meta.setDescription(op.description());
		meta.setApiDesc(op.summary());
		meta.setServiceName(resolveSpringApplicationName());
		// 类上无 @AdminApi/@AppApi/@OpenApi 时暂归 admin；后续可接默认形态配置或方法级元数据
		ApiType declared = ApiContextValidator.declaredClassSurfaceOrNull(handler.getBeanType());
		ApiType effective = declared != null ? declared : ApiType.ADMIN;
		meta.setApiType(effective.getApiType());
		return meta;
	}

	private static boolean matchApiType(ApiMeta api, String apiTypeNorm) {
		if (StringUtils.isBlank(apiTypeNorm)) {
			return true;
		}
		return apiTypeNorm.equals(normalize(api.getApiType()));
	}

	/**
	 * 无 {@link Operation} 或 {@code summary} 为空则返回 {@code null}，不参与汇总。
	 */
	private static OperationDoc extractOperationDoc(HandlerMethod handler) {
		Operation operation = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), Operation.class);
		if (operation == null) {
			operation = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), Operation.class);
		}
		if (operation == null) {
			return null;
		}
		String summary = StringUtils.trimToNull(operation.summary());
		String description = StringUtils.trimToNull(operation.description());
		// 仅写 description 未写 summary 的接口也应进入目录，否则 keyword 永远无法命中说明文字
		if (summary == null && description == null) {
			return null;
		}
		if (summary == null) {
			summary = description;
		}
		return new OperationDoc(summary, description);
	}

	private record OperationDoc(String summary, String description) {
	}

	private static List<String> extractPatterns(RequestMappingInfo info) {
		PathPatternsRequestCondition pathPatterns = info.getPathPatternsCondition();
		if (pathPatterns != null && !pathPatterns.getPatterns().isEmpty()) {
			return pathPatterns.getPatterns().stream().map(p -> p.getPatternString()).collect(Collectors.toList());
		}
		// 使用 Ant 风格路径条件时 PathPatterns 可能为空，若不回退则 urlPath 恒空，keyword 无法按路径匹配
		PatternsRequestCondition ant = info.getPatternsCondition();
		if (ant != null && !ant.getPatterns().isEmpty()) {
			return new ArrayList<>(ant.getPatterns());
		}
		return List.of();
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
				|| normalize(api.getDescription()).contains(keywordNorm)
				|| normalize(api.getServiceName()).contains(keywordNorm) || normalize(api.getApiType()).contains(keywordNorm);
	}

	/**
	 * 网关或前端可能把查询串以「字面百分号」形式传到服务端，与已解码的中文描述无法 contains 匹配；此处按需 UTF-8 解码（含少量轮次以应对双重编码）。
	 */
	private static String decodeKeywordIfPercentEncodedLiteral(String keyword) {
		if (StringUtils.isBlank(keyword)) {
			return keyword;
		}
		String s = keyword;
		for (int round = 0; round < 5 && containsPercentEncodedOctet(s); round++) {
			try {
				String decoded = URLDecoder.decode(s, StandardCharsets.UTF_8);
				if (decoded.equals(s)) {
					break;
				}
				s = decoded;
			}
			catch (IllegalArgumentException ex) {
				break;
			}
		}
		return s;
	}

	private static boolean containsPercentEncodedOctet(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHex(char c) {
		return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
	}

	private static String normalize(String str) {
		return StringUtils.trimToEmpty(str).toLowerCase(Locale.ROOT);
	}
}
