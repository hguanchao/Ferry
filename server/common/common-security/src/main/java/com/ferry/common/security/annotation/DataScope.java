package com.ferry.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限
 *
 * <p>标注在 Mapper 接口上，该接口的查询会按当前用户的数据范围追加过滤条件。
 * 标在接口而不是方法上：MyBatis 的 mappedStatementId 就是「接口全名.方法名」，
 * 反射拿得到注解；而且一个 Mapper 对应一张主表，不会误伤同一次查询里的关联表。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataScope {

    /**
     * 需要按数据范围过滤的表名
     *
     * @return 表名
     */
    String[] value();

    /**
     * 部门字段名
     *
     * <p>留空时使用全局默认配置；只有表的部门字段不叫默认名时才需要指定。
     *
     * @return 部门字段名
     */
    String deptColumn() default "";

    /**
     * 创建人字段名
     *
     * <p>留空时使用全局默认配置。
     *
     * @return 创建人字段名
     */
    String userColumn() default "";
}
