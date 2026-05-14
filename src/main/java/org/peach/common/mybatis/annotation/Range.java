package org.peach.common.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 区间查询标记注解：标注在实体字段上，表示该列可作为「单字段区间」过滤的目标列。
 * <p>
 * 与 {@link org.peach.common.mybatis.model.vo.RangeVO} 配合时，{@link org.peach.common.mybatis.mapper.BaseMapper#likeSelectBase}
 * 在传入非 {@code null} 的 {@code range} 时由 {@link org.peach.common.mybatis.mapper.SelectSqlProvider} 拼接区间 SQL；
 * 传 {@code null} 则不追加区间条件。自定义 {@link org.apache.ibatis.annotations.SelectProvider} 亦可复用
 * {@link org.peach.common.mybatis.mapper.CommonSqlProvider#appendRangeConditions}。
 * </p>
 *
 * @author leiyangjun
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
}

