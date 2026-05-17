package org.peach.common.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.DefaultIdGenerator;

import me.ahoo.cosid.snowflake.MillisecondSnowflakeId;

/**
 * 主键 / 分布式 ID 工具。
 * <ul>
 * <li>{@link #snowId()} — 标准雪花 {@code long}，十进制约 18～19 位，趋势递增（CosId 实现，极少场景保留）</li>
 * <li>{@link #shortSnowId()} — 短雪花 {@code long}，十进制约 12～16 位，数值通常小于 {@link #snowId()}；<b>业务主键默认策略</b></li>
 * <li>{@link #shortId22()} — 22 字符 URL 安全随机串，无时间序，适合 String 主键</li>
 * <li>{@link #randomUuid32()} — 32 位无连字符 UUID 字符串</li>
 * </ul>
 * <p>
 * <b>调用链</b>：{@link org.peach.common.mybatis.mapper.InsertSqlProvider} 插入前调用
 * {@link org.peach.common.mybatis.mapper.CommonSqlProvider#ensureApplicationGeneratedPrimaryKey(Object)}，
 * 空主键时整型写入 {@link #shortSnowId()}、字符串写入 {@link #shortId22()}；
 * 动态 SQL 缺省主键片段见
 * {@link org.peach.common.mybatis.mapper.CommonSqlProvider#getTableKeyValue(Object, boolean)}（规则一致）。
 * 业务代码也可直接调用本类静态方法。
 * </p>
 * <p>
 * <b>配置</b>：系统属性 {@code peach.mybatis.snowflake.worker-id}、
 * {@code peach.mybatis.snowflake.datacenter-id}，默认均为 1（标准雪花各 0～31；短雪花将二者折叠为 4 位
 * WorkerId，最大 15）。纪元 {@link #PEACH_SNOWFLAKE_EPOCH_MS} 为 2021-01-01 00:00:00 UTC。
 * </p>
 */
public final class IdUtil {

	/** 雪花纪元起点（毫秒），2021-01-01 00:00:00 UTC。 */
	static final long PEACH_SNOWFLAKE_EPOCH_MS = 1609459200000L;

	private static final int TWITTER_TIMESTAMP_BITS = 41;
	/** Twitter 经典布局：10 位 machineId = 5 位 datacenter + 5 位 worker。 */
	private static final int TWITTER_MACHINE_BITS = 10;
	private static final int TWITTER_SEQUENCE_BITS = 12;

	private static final long MAX_TWITTER_WORKER_ID = 31L;
	private static final long MAX_TWITTER_DATACENTER_ID = 31L;

	/** 短雪花 WorkerId 位宽（与 {@link #SHORT_SEQ_BIT_LENGTH} 之和须 ≤ 22）。 */
	private static final int SHORT_WORKER_BIT_LENGTH = 4;
	private static final int SHORT_SEQ_BIT_LENGTH = 4;
	private static final long MAX_SHORT_WORKER_ID = (1L << SHORT_WORKER_BIT_LENGTH) - 1L;

	private static final long WORKER_ID = parseLongProp("peach.mybatis.snowflake.worker-id", 1L,
			MAX_TWITTER_WORKER_ID);
	private static final long DATACENTER_ID = parseLongProp("peach.mybatis.snowflake.datacenter-id", 1L,
			MAX_TWITTER_DATACENTER_ID);

	private static final MillisecondSnowflakeId STANDARD_SNOWFLAKE = new MillisecondSnowflakeId(
			PEACH_SNOWFLAKE_EPOCH_MS, TWITTER_TIMESTAMP_BITS, TWITTER_MACHINE_BITS, TWITTER_SEQUENCE_BITS,
			packTwitterMachineId(DATACENTER_ID, WORKER_ID));

	private static final DefaultIdGenerator SHORT_SNOWFLAKE = createShortSnowflakeGenerator();

	private static final SecureRandom RANDOM = new SecureRandom();

	private IdUtil() {
	}

	private static int packTwitterMachineId(long datacenterId, long workerId) {
		return (int) ((datacenterId << 5) | (workerId & 0x1FL));
	}

	private static DefaultIdGenerator createShortSnowflakeGenerator() {
		short yitterWorkerId = (short) Math.min(MAX_SHORT_WORKER_ID,
				(datacenterIdFold() << 2) | (WORKER_ID & 0x3L));
		IdGeneratorOptions options = new IdGeneratorOptions(yitterWorkerId);
		options.WorkerIdBitLength = SHORT_WORKER_BIT_LENGTH;
		options.SeqBitLength = SHORT_SEQ_BIT_LENGTH;
		options.BaseTime = PEACH_SNOWFLAKE_EPOCH_MS;
		return new DefaultIdGenerator(options);
	}

	private static long datacenterIdFold() {
		return Math.min(3L, DATACENTER_ID);
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
	 * 生成下一个标准雪花 ID（线程安全）。
	 * <p>
	 * 41 位毫秒时间戳 + 5 位 datacenter + 5 位 worker + 12 位序列；十进制约 18～19 位。
	 * 示例：{@code long id = IdUtil.snowId();}
	 * </p>
	 */
	public static long snowId() {
		return STANDARD_SNOWFLAKE.generate();
	}

	/**
	 * 生成下一个短雪花 ID（线程安全）。
	 * <p>
	 * WorkerId 4 位、序列 4 位，十进制约 12～16 位，数值通常小于同纪元下的 {@link #snowId()}；
	 * 仍为趋势递增 {@code long}。与 {@link #shortId22()} 不同，具备时间/worker 语义。
	 * 示例：{@code long id = IdUtil.shortSnowId();}
	 * </p>
	 */
	public static long shortSnowId() {
		return SHORT_SNOWFLAKE.newLong();
	}

	/**
	 * 32 位无连字符 UUID 字符串。
	 * <p>
	 * 示例：{@code String uuid = IdUtil.randomUuid32();}
	 * </p>
	 */
	public static String randomUuid32() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * 固定 22 字符的 URL 安全随机串（16 字节熵，Base64 无填充）。
	 * <p>
	 * 无时间/worker 语义；
	 * {@link org.peach.common.mybatis.mapper.CommonSqlProvider} 对 String 主键的默认策略。
	 * 若需趋势有序且数值更短，使用 {@link #shortSnowId()}。
	 * 示例：{@code String id = IdUtil.shortId22();}
	 * </p>
	 */
	public static String shortId22() {
		byte[] buf = new byte[16];
		RANDOM.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}

}
