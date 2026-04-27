package org.peach.common.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 主键 / 分布式 ID 工具：合并原 {@code Uids}、{@code SnowflakeIds}、{@code Uuids} 能力。
 * <ul>
 * <li>数值型：{@link #nextId()}（雪花算法，64 位）</li>
 * <li>字符串型：{@link #randomUuid32()}、{@link #shortId22()}</li>
 * </ul>
 * <p>
 * 雪花 datacenter/worker 取自系统属性 {@code peach.mybatis.snowflake.datacenter-id}、
 * {@code peach.mybatis.snowflake.worker-id}，默认均为 1。
 * </p>
 *
 * @author leiyangjun
 */
public final class IdUtil {

	private static final long EPOCH_MS = 1609459200000L;

	private static final long WORKER_ID_BITS = 5L;
	private static final long DATACENTER_ID_BITS = 5L;
	private static final long SEQUENCE_BITS = 12L;

	private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
	private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

	private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
	private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
	private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

	private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

	private static final long WORKER_ID = parseLongProp("peach.mybatis.snowflake.worker-id", 1L, MAX_WORKER_ID);
	private static final long DATACENTER_ID = parseLongProp("peach.mybatis.snowflake.datacenter-id", 1L,
			MAX_DATACENTER_ID);

	private static long sequence;
	private static long lastTimestamp = -1L;

	private static final SecureRandom RANDOM = new SecureRandom();

	private IdUtil() {
	}

	private static long parseLongProp(String key, long defaultVal, long max) {
		String v = System.getProperty(key);
		if (v == null || v.isEmpty()) {
			return defaultVal;
		}
		try {
			long n = Long.parseLong(v.trim());
			return Math.max(0L, Math.min(n, max));
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	/**
	 * 生成下一个雪花 ID（单机线程安全；多实例请配置不同 worker/datacenter）。
	 */
	public static synchronized long nextId() {
		long ts = System.currentTimeMillis();
		if (ts < lastTimestamp) {
			throw new IllegalStateException(
					"时钟回拨，拒绝生成 ID。last=" + lastTimestamp + ", now=" + ts);
		}
		if (ts == lastTimestamp) {
			sequence = (sequence + 1) & SEQUENCE_MASK;
			if (sequence == 0) {
				ts = waitNextMillis(lastTimestamp);
			}
		} else {
			sequence = 0L;
		}
		lastTimestamp = ts;
		return ((ts - EPOCH_MS) << TIMESTAMP_SHIFT)
				| (DATACENTER_ID << DATACENTER_ID_SHIFT)
				| (WORKER_ID << WORKER_ID_SHIFT)
				| sequence;
	}

	private static long waitNextMillis(long last) {
		long ts = System.currentTimeMillis();
		while (ts <= last) {
			ts = System.currentTimeMillis();
		}
		return ts;
	}

	/**
	 * 32 位无连字符 UUID 字符串。
	 */
	public static String randomUuid32() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * 固定 22 字符的 URL 安全随机串：16 字节熵经 Base64（无填充）编码，长度恒为 22。
	 */
	public static String shortId22() {
		byte[] buf = new byte[16];
		RANDOM.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}
}

