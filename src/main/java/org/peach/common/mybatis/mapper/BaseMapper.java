package org.peach.common.mybatis.mapper;

import java.io.Serializable;
import java.util.List;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import org.peach.common.mybatis.model.vo.BigPageVO;
import org.peach.common.mybatis.model.vo.CommonQueryVO;
import org.peach.common.mybatis.model.vo.SortVO;

/**
 * 通用 Mapper 基接口：在单表实体上封装增删改查、批量与唯一性校验等操作，具体 SQL 由
 * {@link org.peach.common.mybatis.mapper.BaseSqlProvider} 按注解与入参动态生成。
 * <p>
 * 精确列表查询为「非 null 字段等值匹配」，不包含模糊与 {@link CommonQueryVO} 区间；
 * 模糊与区间请使用 {@link #likeSelectBase}。列表相关方法排序由 {@link SortVO} 传入（可 {@code null}）。
 * </p>
 *
 * @param <T> 与表映射的实体类型，须可序列化
 * @author leiyangjun
 * @since 0.0.1-SNAPSHOT
 */
public interface BaseMapper<T extends Serializable> {

	/**
	 * 插入（忽略 null 列）。
	 * <p>
	 * 差异对照（vs {@link #insertBaseAll}）：本方法仅将<strong>非 null</strong>普通字段写入 INSERT，字段为 null 时不出现在 SQL 中，
	 * 因而数据库默认值可生效；主键与逻辑删除列仍按 Provider 规则处理（显式值优先，否则尝试默认策略）。
	 * </p>
	 *
	 * @param entity 表对应实体
	 * @return 受影响行数
	 */
	@InsertProvider(type = BaseSqlProvider.class, method = "insertBaseSQL")
	Integer insertBase(T entity);

	/**
	 * 插入（包含 null 列）。
	 * <p>
	 * 差异对照（vs {@link #insertBase}）：本方法会将普通字段统一写入 INSERT，即使属性值为 null 也写入数据库 NULL，
	 * 通常用于“整单提交/全字段落库”；主键与逻辑删除列处理规则与 {@link #insertBase} 一致。
	 * </p>
	 *
	 * @param entity 表对应实体
	 * @return 受影响行数
	 */
	@InsertProvider(type = BaseSqlProvider.class, method = "insertBaseAllSQL")
	Integer insertBaseAll(T entity);

	/**
	 * 按实体中非 null 字段作为等值条件执行物理删除（可能多条），空串也会参与匹配；请谨慎使用。
	 *
	 * @param entity 条件实体，不需要参与匹配的字段请置为 {@code null}
	 * @return 受影响行数
	 */
	@DeleteProvider(type = BaseSqlProvider.class, method = "deleteBaseSQL")
	Integer deleteBase(T entity);

	/**
	 * 按主键物理删除单条记录（不支持复合主键）。
	 *
	 * @param key     主键值，不可为 {@code null} 或空串
	 * @param voClass 实体类型，须能解析出 {@link org.peach.common.mybatis.annotation.ID} 对应列
	 * @return 受影响行数
	 */
	@DeleteProvider(type = BaseSqlProvider.class, method = "deleteBaseByKeySQL")
	<U> Integer deleteBaseByKey(@Param("key") Serializable key, @Param("voClass") Class<?> voClass);

	/**
	 * 按主键更新（忽略 null 字段）。
	 * <p>
	 * 差异对照（vs {@link #updateBaseAll}）：仅对<strong>非 null</strong>且非主键字段生成 SET，null 字段不会覆盖库中原值，
	 * 适合 PATCH/局部更新；仍会排除创建审计字段（以 Provider 规则为准）。
	 * </p>
	 *
	 * @param entity 含主键及待更新字段的实体
	 * @return 受影响行数
	 */
	@UpdateProvider(type = BaseSqlProvider.class, method = "updateBaseSQL")
	Integer updateBase(T entity);

	/**
	 * 按主键更新（包含 null 字段）。
	 * <p>
	 * 差异对照（vs {@link #updateBase}）：非主键字段都会生成 SET，属性值为 null 时会把数据库列更新为 NULL，
	 * 适合“前端全量回传 -> 后端整单覆盖”场景；仍会排除创建审计字段（以 Provider 规则为准）。
	 * </p>
	 *
	 * @param entity 含主键及待更新字段的实体
	 * @return 受影响行数
	 */
	@UpdateProvider(type = BaseSqlProvider.class, method = "updateBaseAllSQL")
	Integer updateBaseAll(T entity);

	/**
	 * 条件查询（标准列策略）：非 null 等值 + 可选 {@link SortVO}（不含模糊、不含 {@link CommonQueryVO} 区间）。
	 * <p>
	 * 差异对照（vs {@link #selectBaseAll}）：主要差异在列集合策略，本方法使用 {@code getTableColumns(entity, true)}。
	 * </p>
	 *
	 * @param entity 查询条件实体（等值条件）
	 * @param sort   排序（可为 {@code null}）
	 * @return 结果列表
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "selectBaseSQL")
	List<T> selectBase(@Param("entity") T entity, @Param("sort") SortVO sort);

	/**
	 * 与 {@link #selectBase} WHERE 语义相同，仅返回第一条（{@code LIMIT 1}）；不生成 {@code ORDER BY}，多条命中时由数据库决定返回哪一条。
	 *
	 * @param entity 查询条件实体（等值条件）
	 * @return 单条记录，可能为 {@code null}
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "selectBaseOneSQL")
	T selectBaseOne(@Param("entity") T entity);

	/**
	 * 条件查询（全列策略）：语义同 {@link #selectBase}，列集合为 {@code getTableColumns(entity)}。
	 *
	 * @param entity 查询条件实体（等值条件）
	 * @param sort   排序（可为 {@code null}）
	 * @return 结果列表
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "selectBaseAll")
	List<T> selectBaseAll(@Param("entity") T entity, @Param("sort") SortVO sort);

	/**
	 * 大数据量游标分页：固定按主键升序，基于 {@link BigPageVO#lastId} 向后翻页。
	 * <p>
	 * 规则：当 {@code lastId} 为空时取第一页；否则追加 {@code 主键 > lastId} 条件，并按主键升序 {@code LIMIT pageSize}。
	 * </p>
	 *
	 * @param entity  等值条件实体（非 null 字段参与 WHERE）
	 * @param bigPage 游标分页参数（lastId/pageSize）
	 * @return 结果列表
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "bigPageBaseSQL")
	List<T> bigPageBase(@Param("entity") T entity, @Param("bigPage") BigPageVO bigPage);

	/**
	 * 批量插入：各实体字段集合应一致；主键策略适用；通常使用 {@code INSERT IGNORE} 类语义忽略冲突行（以生成 SQL 为准）。
	 *
	 * @param list 待插入实体列表
	 * @return 受影响行数
	 */
	@InsertProvider(type = BaseSqlProvider.class, method = "batchInsertBaseSQL")
	Integer batchInsertBase(List<T> list);

	/**
	 * 按主键批量物理删除：列表中每项须带主键；入参形态较重，一般更推荐 {@link #batchDeleteBaseByKeys}。
	 *
	 * @param list 含主键的实体列表
	 * @return 受影响行数
	 */
	@DeleteProvider(type = BaseSqlProvider.class, method = "batchDeleteBaseSQL")
	Integer batchDeleteBase(List<T> list);

	/**
	 * 按非主键非空字段做唯一性校验，并以主键排除当前行（主键可为空表示新增场景）；VO 中不参与校验的字段须为 {@code null}。
	 *
	 * @param entity 待校验实体
	 * @return 重复时大于 0，否则为 0
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "checkBaseSQL")
	Integer checkBase(T entity);

	/**
	 * 仅针对标有 {@link org.peach.common.mybatis.annotation.Unique} 的字段做组合唯一校验，推荐使用。
	 *
	 * @param entity 待校验实体
	 * @return 重复时大于 0，否则为 0
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "checkUniqueSQL")
	Integer checkUnique(T entity);

	/**
	 * 同 {@link #checkUnique}，返回是否存在重复。
	 *
	 * @param entity 待校验实体
	 * @return 存在重复为 {@code true}
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "checkExistSQL")
	Boolean checkExist(T entity);

	/**
	 * 对多个 {@link org.peach.common.mybatis.annotation.Unique} 字段做「或」语义下的重复计数（以 Provider 生成 SQL 为准）。
	 *
	 * @param entity 待校验实体
	 * @return 重复时大于 0，否则为 0
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "checkMultOrUniqueSQL")
	Integer checkMultOrUnique(T entity);

	/**
	 * 同 {@link #checkMultOrUnique}，返回是否存在重复。
	 *
	 * @param entity 待校验实体
	 * @return 存在重复为 {@code true}
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "checkMultOrExistSQL")
	Boolean checkMultOrExist(T entity);

	/**
	 * 支持精确匹配与 {@link org.peach.common.mybatis.annotation.SearchValue} 模糊条件；可与 {@link org.peach.common.mybatis.annotation.Range} 组合。
	 *
	 * @param entity 查询条件实体（等值条件）
	 * @param query  模糊搜索/区间（{@link CommonQueryVO}）
	 * @param sort   排序（可为 {@code null}）
	 * @return 结果列表
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "likeSelectBaseSQL")
	List<T> likeSelectBase(@Param("entity") T entity, @Param("query") CommonQueryVO query, @Param("sort") SortVO sort);

	/**
	 * 按主键查询单条。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 实体或 {@code null}
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "selectBaseByKeySQL")
	<U> T selectBaseByKey(@Param("key") Serializable key, @Param("voClass") Class<?> voClass);

	/**
	 * 按主键集合查询多条。
	 *
	 * @param list    主键列表
	 * @param voClass 实体类型
	 * @return 结果列表
	 */
	@SelectProvider(type = BaseSqlProvider.class, method = "selectBaseByKeysSQL")
	<U> List<T> selectBaseByKeys(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass, @Param("sort") SortVO sort);

	/**
	 * 按主键列表批量物理删除。
	 *
	 * @param list    主键集合
	 * @param voClass 实体类型（须含主键映射）
	 * @return 受影响行数
	 */
	@DeleteProvider(type = BaseSqlProvider.class, method = "batchDeleteBaseByKeysSQL")
	<U> Integer batchDeleteBaseByKeys(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass);

	/**
	 * 按主键逻辑删除单条（更新逻辑删除标记，非物理删）。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 受影响行数
	 */
	@UpdateProvider(type = BaseSqlProvider.class, method = "logicDeleteByKeySQL")
	<U> Integer logicDeleteByKey(@Param("key") Serializable key, @Param("voClass") Class<?> voClass);

	/**
	 * 按主键将逻辑删除标记恢复为有效（如上架、启用等场景）。
	 *
	 * @param key     主键值
	 * @param voClass 实体类型
	 * @return 受影响行数
	 */
	@UpdateProvider(type = BaseSqlProvider.class, method = "logicRecoveryByKeySQL")
	<U> Integer logicRecoveryByKey(@Param("key") Serializable key, @Param("voClass") Class<?> voClass);

	/**
	 * 按主键列表批量逻辑删除。
	 *
	 * @param list    主键集合
	 * @param voClass 实体类型
	 * @return 受影响行数
	 */
	@UpdateProvider(type = BaseSqlProvider.class, method = "logicBatchDeleteKeysSQL")
	<U> Integer logicBatchDeleteKeys(@Param("list") List<? extends Serializable> list,
			@Param("voClass") Class<?> voClass);
}

