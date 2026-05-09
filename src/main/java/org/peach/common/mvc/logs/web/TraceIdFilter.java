package org.peach.common.mvc.logs.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet 过滤器：为每个 HTTP 请求在 MDC 中放入 traceId（优先使用 {@value #TRACE_ID_HEADER}，缺失则生成紧凑 UUID），
 * 并在响应头回写便于链路关联；请求结束后 {@link MDC#remove(String)}，避免线程池复用污染。
 * <p>
 * 与 {@code logback-spring.xml} 中布局 {@code %X{traceId}} 约定一致。
 * </p>
 *
 * @author leiyangjun
 */
public class TraceIdFilter extends HttpFilter {

	private static final long serialVersionUID = 1L;

	/** 请求与响应中的 trace 头名称（固定约定，与网关/前端对齐即可） */
	public static final String TRACE_ID_HEADER = "X-Trace-Id";

	/** 写入 MDC 的键，须与 Logback 模式中的 {@code %X{...}} 一致 */
	public static final String TRACE_ID_MDC_KEY = "traceId";

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String traceId = request.getHeader(TRACE_ID_HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = compactUuid();
		}
		else {
			traceId = traceId.trim();
			if (traceId.length() > 128) {
				traceId = traceId.substring(0, 128);
			}
		}
		MDC.put(TRACE_ID_MDC_KEY, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(TRACE_ID_MDC_KEY);
		}
	}

	private static String compactUuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
