package org.peach.common.mybatis.service;

import java.io.Serializable;

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

	V save(V vo);

	PageInfo<V> listPage(V condition, PageVO page, SortVO sort);
}
