package org.peach.common.openapi;

import org.peach.common.openapi.autoconfigure.PeachOpenApiConfiguration;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 按访问 Swagger 的方式（经网关或直连）修正 OpenAPI 的 {@code servers}，避免「Try it out」调错地址；
 * 并在<strong>判定为经网关访问</strong>时，将 {@code info.contact.url} 设为指向网关根路径下门户页的<strong>绝对 URL</strong>
 * （Swagger UI 对相对路径联系人链接展示不稳定），无需在配置中写死网关地址。
 */
public class SwaggerOpenApiCustomizer implements OpenApiCustomizer {

	/** 网关在对外域名根路径下提供的 API 门户入口（与网关静态页约定一致）。 */
	private static final String GATEWAY_PORTAL_INDEX_PATH = "/index.html";

	/** 经网关访问时，OpenAPI server URL 使用的路径前缀，形如 {@code /peach-auth-service}。 */
	private final String gatewayPathPrefix;

	private final int serverPort;

	public SwaggerOpenApiCustomizer(String applicationName, int serverPort) {
		this.gatewayPathPrefix = "/" + applicationName.toLowerCase(Locale.ROOT);
		this.serverPort = serverPort;
	}

	@Override
	public void customise(OpenAPI openApi) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			openApi.setServers(List.of(server("/", "无 HTTP 请求上下文（如启动期），默认相对根路径")));
			return;
		}
		HttpServletRequest req = attrs.getRequest();
		String prefix = resolveGatewayPrefixHeader(req);
		if (StringUtils.hasText(prefix)) {
			String url = prefix.trim();
			if (!url.startsWith("/")) {
				url = "/" + url;
			}
			openApi.setServers(List.of(server(url, "判定：经网关（X-Forwarded-Prefix 或 X-Peach-Gateway-Prefix）")));
			applyContactPortalUrl(openApi, req, true);
			return;
		}
		if (refererPathHasServicePrefix(req)) {
			openApi.setServers(List.of(server(gatewayPathPrefix, "判定：Referer 路径含本服务在网关下的前缀")));
			applyContactPortalUrl(openApi, req, true);
			return;
		}
		if (forwardedClientPortDiffersFromServerPort(req)) {
			openApi.setServers(List.of(server(gatewayPathPrefix,
					"判定：X-Forwarded 侧端口与 server.port 不一致（典型经网关访问）")));
			applyContactPortalUrl(openApi, req, true);
			return;
		}
		openApi.setServers(List.of(server("/", "判定：直连本服务（无网关前缀特征）")));
		applyContactPortalUrl(openApi, req, false);
	}

	/**
	 * 经网关：根据转发头拼出门户 {@code index.html} 的绝对 URL；直连：去掉链接避免指向本机无效路径。
	 */
	private void applyContactPortalUrl(OpenAPI openApi, HttpServletRequest req, boolean gatewayAccess) {
		if (openApi.getInfo() == null) {
			return;
		}
		Contact contact = openApi.getInfo().getContact();
		if (contact == null) {
			return;
		}
		if (!SwaggerPortalContact.NAME.equals(contact.getName())) {
			return;
		}
		if (StringUtils.hasText(SwaggerPortalContact.URL)) {
			return;
		}
		if (gatewayAccess) {
			String portal = buildGatewayPortalAbsoluteUrl(req);
			if (StringUtils.hasText(portal)) {
				contact.setUrl(portal);
			}
		}
		else {
			contact.setUrl(null);
		}
	}

	/**
	 * 拼接网关门户 {@code index.html} 的绝对 URL。
	 * <p>
	 * 顺序：显式 {@code X-Forwarded-*} → Spring {@link ServletUriComponentsBuilder}（配合 {@code server.forward-headers-strategy} 与 RFC 7239
	 * {@code Forwarded}）→ {@code Referer}。<strong>不使用</strong>原始 {@code Host}，经网关时它多为实例地址（{@code server.port}），会导致 8084 误链。
	 * </p>
	 */
	private String buildGatewayPortalAbsoluteUrl(HttpServletRequest req) {
		String explicit = buildPortalUrlFromExplicitForwardedHeaders(req);
		if (StringUtils.hasText(explicit)) {
			return explicit;
		}
		String fromSpring = buildPortalUrlFromForwardedRequest(req);
		if (StringUtils.hasText(fromSpring)) {
			return fromSpring;
		}
		return portalUrlFromReferer(req);
	}

	/** 网关全局过滤器写入的 {@code X-Forwarded-*}。 */
	private static String buildPortalUrlFromExplicitForwardedHeaders(HttpServletRequest req) {
		String scheme = firstCsvToken(req.getHeader("X-Forwarded-Proto"), null);
		String xfHost = firstCsvToken(req.getHeader("X-Forwarded-Host"), null);
		if (!StringUtils.hasText(scheme) || !StringUtils.hasText(xfHost)) {
			return null;
		}
		return normalizeForwardedAuthority(scheme, xfHost, req) + GATEWAY_PORTAL_INDEX_PATH;
	}

	/**
	 * 使用 Boot 对 {@code Forwarded} / {@code X-Forwarded-*} 解析后的请求 URI；若解析结果端口仍等于本服务 {@link #serverPort} 且存在网关前缀头，
	 * 说明转发链未生效，返回 null 交由 Referer。
	 */
	private String buildPortalUrlFromForwardedRequest(HttpServletRequest req) {
		try {
			UriComponents uc = ServletUriComponentsBuilder.fromRequest(req).build();
			String scheme = uc.getScheme();
			String host = uc.getHost();
			if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
				return null;
			}
			int effectivePort = effectivePort(uc);
			/* 本方法仅在「已判定经网关」时调用；若解析结果端口仍为本容器 server.port，说明未吃到转发链，勿当作门户地址 */
			if (effectivePort == serverPort) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(scheme).append("://").append(host);
			if (effectivePort > 0 && !isDefaultPort(scheme, effectivePort)) {
				sb.append(':').append(effectivePort);
			}
			sb.append(GATEWAY_PORTAL_INDEX_PATH);
			return sb.toString();
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static int effectivePort(UriComponents uc) {
		int p = uc.getPort();
		if (p > 0) {
			return p;
		}
		String s = uc.getScheme();
		if ("https".equalsIgnoreCase(s)) {
			return 443;
		}
		if ("http".equalsIgnoreCase(s)) {
			return 80;
		}
		return -1;
	}

	/**
	 * {@code X-Forwarded-Host} 仅有主机名时，用 {@code X-Forwarded-Port} 补端口（非默认端口才追加）。
	 */
	private static String normalizeForwardedAuthority(String scheme, String forwardedHost, HttpServletRequest req) {
		String authority = forwardedHost.trim();
		if (!authority.contains(":")) {
			String fp = firstCsvToken(req.getHeader("X-Forwarded-Port"), null);
			if (StringUtils.hasText(fp)) {
				try {
					int p = Integer.parseInt(fp.split(",")[0].trim());
					if (p > 0 && !isDefaultPort(scheme, p)) {
						authority = authority + ":" + p;
					}
				}
				catch (NumberFormatException ex) {
					// 忽略非法端口，仅用主机名
				}
			}
		}
		return scheme + "://" + authority;
	}

	private static boolean isDefaultPort(String scheme, int port) {
		if ("https".equalsIgnoreCase(scheme)) {
			return port == 443;
		}
		return port == 80;
	}

	/** 从 Referer 取 scheme/host/port，得到与浏览器当前访问网关一致的门户根 URL。 */
	private static String portalUrlFromReferer(HttpServletRequest req) {
		String ref = req.getHeader("Referer");
		if (!StringUtils.hasText(ref)) {
			return null;
		}
		try {
			URI u = URI.create(ref.trim());
			if (u.getScheme() == null || u.getHost() == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(u.getScheme()).append("://").append(u.getHost());
			int p = u.getPort();
			if (p > 0) {
				sb.append(':').append(p);
			}
			sb.append(GATEWAY_PORTAL_INDEX_PATH);
			return sb.toString();
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static String firstCsvToken(String header, String fallback) {
		if (StringUtils.hasText(header)) {
			return header.split(",")[0].trim();
		}
		return fallback != null ? fallback.trim() : "";
	}

	private String resolveGatewayPrefixHeader(HttpServletRequest req) {
		String a = req.getHeader("X-Forwarded-Prefix");
		if (StringUtils.hasText(a)) {
			return a;
		}
		return req.getHeader("X-Peach-Gateway-Prefix");
	}

	private boolean refererPathHasServicePrefix(HttpServletRequest req) {
		String ref = req.getHeader("Referer");
		if (!StringUtils.hasText(ref)) {
			return false;
		}
		try {
			URI uri = URI.create(ref);
			String p = uri.getPath();
			return p != null && (p.startsWith(gatewayPathPrefix + "/") || p.equals(gatewayPathPrefix));
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private boolean forwardedClientPortDiffersFromServerPort(HttpServletRequest req) {
		String xp = req.getHeader("X-Forwarded-Port");
		if (StringUtils.hasText(xp)) {
			int p = parseFirstPort(xp);
			if (p > 0 && p != serverPort) {
				return true;
			}
		}
		String xh = req.getHeader("X-Forwarded-Host");
		if (StringUtils.hasText(xh)) {
			int p = parseHostPortFromForwardedHost(xh);
			if (p > 0 && p != serverPort) {
				return true;
			}
		}
		return false;
	}

	private static int parseFirstPort(String header) {
		String part = header.split(",")[0].trim();
		try {
			return Integer.parseInt(part);
		}
		catch (NumberFormatException ex) {
			return -1;
		}
	}

	private static int parseHostPortFromForwardedHost(String header) {
		String part = header.split(",")[0].trim();
		int colon = part.lastIndexOf(':');
		if (colon < 0 || colon == part.length() - 1) {
			return -1;
		}
		try {
			return Integer.parseInt(part.substring(colon + 1));
		}
		catch (NumberFormatException ex) {
			return -1;
		}
	}

	private static Server server(String url, String description) {
		Server s = new Server();
		s.setUrl(url);
		s.setDescription(description);
		return s;
	}
}
