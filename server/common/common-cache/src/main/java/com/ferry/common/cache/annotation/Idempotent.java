package com.ferry.common.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 幂等/防重复提交注解。
 *
 * <p>在方法入口用 SET NX PX 抢占幂等键，同一 key 在 TTL 内重复调用直接拒绝。
 * 适用于按钮重复点击、网络重试等短窗口防重场景；业务唯一性仍需数据库约束/状态判断兜底。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Idempotent {

    /**
     * 幂等键 SpEL 表达式，如 {@code "#body.requestId"}；为空时对方法签名+参数做 SHA-256。
     */
    String key() default "";

    /** 幂等窗口秒数 */
    int ttl() default 60;

    /** 重复命中提示 */
    String message() default "请勿重复提交";
}
