package org.peach.common.discovery;



/**

 * 注册中心实例元数据键名（与网关文档门户读取字段一致）。

 */

public final class PeachDiscoveryMetadataKeys {



	private PeachDiscoveryMetadataKeys() {

	}



	/**

	 * 服务说明，可选；与本地配置 {@code server.description} 同名，写入 Nacos 实例 metadata，

	 * 网关门户从该键读取「服务说明」列。

	 */

	public static final String DESCRIPTION = "server.description";



	/**

	 * 中间版本 starter 曾写入的键，仅网关在读取元数据时作为兼容回退。

	 */

	public static final String DESCRIPTION_COMPAT_PEACH = "peach.description";



	/**

	 * 更早键名 {@code peach.service-description}，仅网关兼容回退。

	 */

	public static final String DESCRIPTION_LEGACY = "peach.service-description";

}


