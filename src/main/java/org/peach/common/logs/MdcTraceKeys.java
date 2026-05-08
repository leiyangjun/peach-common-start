package org.peach.common.logs;

/**
 * MDC 中与链路追踪相关的键名约定（默认与 {@link PeachLoggingProperties.Trace#mdcKey} 一致）。
 */
public final class MdcTraceKeys {

	/** 默认 traceId 在 MDC 中的键 */
	public static final String TRACE_ID = "traceId";

	private MdcTraceKeys() {
	}
}
