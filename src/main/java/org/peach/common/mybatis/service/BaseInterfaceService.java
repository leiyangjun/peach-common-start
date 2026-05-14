package org.peach.common.mybatis.service;

import java.io.Serializable;

import org.peach.common.mybatis.model.vo.SearchVO;
import org.peach.common.mybatis.model.vo.PageVO;
import org.peach.common.mybatis.model.vo.SortVO;

import com.github.pagehelper.PageInfo;

/**
 * 面向 VO 的通用服务契约（查询、保存、分页），供业务 Service 接口继承。
 * <p>
 * 不包含物理删除：部分业务禁止删库或仅允许逻辑删除，删除能力应由具体业务在 Service/Mapper 层按需暴露。
 * </p>
 *
 * @param <V> 业务 VO 类型
 * @author leiyangjun
 */
public interface BaseInterfaceService<V extends Serializable> {

	V getById(Serializable id);

	/**
	 * 新增或更新后返回<b>主键</b>（与表主键类型一致，常见为 {@link Long}）。
	 */
	Serializable save(V vo);

	/**
	 * 分页列表：等值条件来自 {@code condition}；关键字模糊见 {@code searchVO}（可 {@code null}）。
	 * <p>
	 * 不在基类契约中传入区间对象：并非所有查询都需要范围过滤；需要日期/数值区间时由具体业务在 Service/Mapper 层扩展自定义查询。
	 * </p>
	 */
	PageInfo<V> listPage(V condition, SearchVO searchVO, PageVO page, SortVO sort);
}
