package com.ferry.common.web.annotation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 微服务启动注解。
 *
 * <p>只负责服务自身的组件扫描与注册发现：公共能力由各 common 模块的自动装配提供，
 * 这里不再扫描公共包，避免同一个配置类被扫描与自动装配重复注册。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootApplication
@EnableDiscoveryClient
public @interface CloudApplication {
}
