package com.ferry.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限校验注解
 *
 * <p>标注在方法或类上，调用前校验当前登录用户是否持有任一所需权限标识。
 * 未标注的接口不做权限校验，鉴权只对显式声明生效，因此新增接口时必须自行决定是否标注。
 *
 * <p>方法级注解优先于类级注解：方法上存在该注解时，类上的声明不再参与判断。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PreAuthorize {

    /**
     * 所需权限标识，持有其中任意一个即放行
     *
     * <p>标识格式为「模块:资源:动作」，格式中的某一段允许写成 {@code *} 通配。
     * 用户持有的标识与这里声明的标识都会按段比对，两侧都可以带通配。
     *
     * @return 权限标识数组
     */
    String[] value();
}
