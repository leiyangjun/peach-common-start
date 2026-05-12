package org.peach.common.mvc.result.code;

/**
 * 下游业务服务 {@link MessageCode} 消息码段校验（与 {@link org.peach.common.mvc.exception.BizException} 共用）。
 * <p>
 * 约定：参与拼接的四位消息码段 {@code h}（见 {@link #normalizeMsgSegment(int)}，取末四位非负值）：
 * <ul>
 *   <li>HTTP 400 语义：{@code 4100 ≤ h ≤ 4999}</li>
 *   <li>HTTP 500 语义：{@code 5100 ≤ h ≤ 5999}</li>
 * </ul>
 * 启动器框架内置枚举（{@link MessageCode#frameworkBuiltinMessageCode()} 为 {@code true}）不走本类校验。
 * </p>
 *
 * @author leiyangjun
 */
public final class ApiResultCodeRules {

	private ApiResultCodeRules() {
	}

	/** 业务服务：HTTP 400 族消息码段下界（含）。 */
	public static final int APPLICATION_BIZ_400_MIN = 4100;
	/** 业务服务：HTTP 400 族消息码段上界（含）。 */
	public static final int APPLICATION_BIZ_400_MAX = 4999;

	/** 业务服务：HTTP 500 族消息码段下界（含）。 */
	public static final int APPLICATION_BIZ_500_MIN = 5100;
	/** 业务服务：HTTP 500 族消息码段上界（含）。 */
	public static final int APPLICATION_BIZ_500_MAX = 5999;

	/**
	 * 将任意 int 规范为「末四位」非负段（{@code 0–9999}），与枚举中书写习惯一致。
	 */
	public static int normalizeMsgSegment(int msgCode) {
		return Math.floorMod(msgCode, 10000);
	}

	/**
	 * 业务服务自定义码：HTTP 400 族，消息码段须在 {@link #APPLICATION_BIZ_400_MIN}–{@link #APPLICATION_BIZ_400_MAX}。
	 */
	public static void assertApplicationBiz400(int msgCode) {
		int h = normalizeMsgSegment(msgCode);
		if (h < APPLICATION_BIZ_400_MIN || h > APPLICATION_BIZ_400_MAX) {
			throw new IllegalArgumentException("业务服务 HTTP 400 消息码段须在 " + APPLICATION_BIZ_400_MIN + "–" + APPLICATION_BIZ_400_MAX
					+ " 内，当前=" + h);
		}
	}

	/**
	 * 业务服务自定义码：HTTP 500 族，消息码段须在 {@link #APPLICATION_BIZ_500_MIN}–{@link #APPLICATION_BIZ_500_MAX}。
	 */
	public static void assertApplicationBiz500(int msgCode) {
		int h = normalizeMsgSegment(msgCode);
		if (h < APPLICATION_BIZ_500_MIN || h > APPLICATION_BIZ_500_MAX) {
			throw new IllegalArgumentException("业务服务 HTTP 500 消息码段须在 " + APPLICATION_BIZ_500_MIN + "–" + APPLICATION_BIZ_500_MAX
					+ " 内，当前=" + h);
		}
	}
}
