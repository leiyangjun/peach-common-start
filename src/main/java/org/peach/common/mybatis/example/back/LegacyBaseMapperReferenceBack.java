package org.peach.common.mybatis.example.back;

import java.io.Serializable;

/**
 * 占位说明类：原工程 {@code com.idea.platform.common.mapper.BaseMapper} 逻辑已迁移为
 * {@link org.peach.common.mybatis.mapper.BaseMapper}，SQL 片段由 {@link org.peach.common.mybatis.mapper.BaseSqlProvider} 提供。
 * <p>
 * 保留本类仅为满足「example 包参考代码加 back 后缀」的目录约定，不参与业务继承。
 * </p>
 *
 * @author leiyangjun
 */
public final class LegacyBaseMapperReferenceBack {

	private LegacyBaseMapperReferenceBack() {
	}

	/**
	 * 与历史 BaseMapper 泛型约束一致。
	 */
	public interface Marker extends Serializable {
	}
}

