package org.peach.common.logs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Peach 统一日志配置（文件路径、归档、链路 traceId、按环境默认级别等）。
 * <p>
 * 根级别及包级别默认由 {@link org.peach.common.logs.autoconfigure.PeachLoggingEnvironmentPostProcessor} 按
 * {@code spring.profiles.active} 注入（低优先级，业务 {@code application.yml} 可覆盖）。
 * 亦可显式使用 {@code logging.level.*}。
 * </p>
 */
@ConfigurationProperties(prefix = "peach.logging")
public class PeachLoggingProperties {

	/**
	 * 是否启用本 starter 提供的 traceId 过滤器与默认 logback 约定（关闭后仅不注册 Filter，仍可使用自建 logback）。
	 */
	private boolean enabled = true;

	private final File file = new File();

	private final Trace trace = new Trace();

	private final Dev dev = new Dev();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public File getFile() {
		return file;
	}

	public Trace getTrace() {
		return trace;
	}

	public Dev getDev() {
		return dev;
	}

	/**
	 * 日志文件目录（相对路径则相对于 {@code user.dir}，一般为进程工作目录 / 应用根目录）。
	 */
	public static class File {

		/**
		 * 日志根目录，其下生成 console.log、error.log 及 archive/ 归档。
		 */
		private String path = "logs";

		/**
		 * 主日志（console.log）归档保留天数。
		 */
		private int mainMaxHistory = 30;

		/**
		 * 错误日志归档保留天数。
		 */
		private int errorMaxHistory = 90;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public int getMainMaxHistory() {
			return mainMaxHistory;
		}

		public void setMainMaxHistory(int mainMaxHistory) {
			this.mainMaxHistory = mainMaxHistory;
		}

		public int getErrorMaxHistory() {
			return errorMaxHistory;
		}

		public void setErrorMaxHistory(int errorMaxHistory) {
			this.errorMaxHistory = errorMaxHistory;
		}
	}

	/**
	 * HTTP 请求链路 traceId，写入 MDC 与响应头，便于检索同一次请求的全链路日志。
	 */
	public static class Trace {

		private boolean enabled = true;

		private String headerName = "X-Trace-Id";

		private String mdcKey = "traceId";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getHeaderName() {
			return headerName;
		}

		public void setHeaderName(String headerName) {
			this.headerName = headerName;
		}

		public String getMdcKey() {
			return mdcKey;
		}

		public void setMdcKey(String mdcKey) {
			this.mdcKey = mdcKey;
		}
	}

	/**
	 * 非生产、非 test  profile 时生效：为指定包注入 DEBUG（可被 {@code logging.level.*} 覆盖）。
	 */
	public static class Dev {

		/**
		 * 开发环境下调为 DEBUG 的包前缀；默认含平台包 {@code org.peach}，下游可追加自身业务根包。
		 */
		private List<String> debugPackages = new ArrayList<>(List.of("org.peach"));

		public List<String> getDebugPackages() {
			return debugPackages;
		}

		public void setDebugPackages(List<String> debugPackages) {
			this.debugPackages = debugPackages;
		}
	}
}
