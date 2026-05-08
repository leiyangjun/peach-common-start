package org.peach.common.logs.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 为每个 HTTP 请求注入 traceId：优先读取上游请求头，否则生成 UUID；请求结束清理 MDC。
 */
public class TraceIdFilter extends HttpFilter {

	private static final long serialVersionUID = 1L;

	private final String headerName;
	private final String mdcKey;

	public TraceIdFilter(String headerName, String mdcKey) {
		this.headerName = headerName;
		this.mdcKey = mdcKey;
	}

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String traceId = request.getHeader(headerName);
		if (traceId == null || traceId.isBlank()) {
			traceId = compactUuid();
		}
		else {
			traceId = traceId.trim();
			if (traceId.length() > 128) {
				traceId = traceId.substring(0, 128);
			}
		}
		MDC.put(mdcKey, traceId);
		response.setHeader(headerName, traceId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(mdcKey);
		}
	}

	private static String compactUuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
