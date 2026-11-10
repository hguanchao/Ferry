package com.ferry.common.core.utils;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring Application工具类
 */
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * 注入上下文，仅在首次赋值，避免多容器场景相互覆盖
     *
     * <p>参数标注跟随 {@link ApplicationContextAware} 所在的包级 {@code @NullMarked} 契约，
     * 不属于本类的自定义约束。
     *
     * @param context Spring 上下文，由容器保证非空
     * @throws BeansException 注入失败
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        if (SpringUtil.context == null) {
            SpringUtil.context = context;
        }
    }

    /**
     * 获取bean对象
     *
     * @param clazz class类
     * @param <T>   泛型
     * @return bean对象
     */
    public static <T> T getBean(Class<T> clazz) {
        if (clazz == null || context == null) {
            return null;
        }
        return context.getBean(clazz);
    }

    /**
     * 获取bean对象
     *
     * @param beanId bean对象id
     * @param <T>    泛型
     * @return bean对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String beanId) {
        if (StrUtil.isBlank(beanId) || context == null) {
            return null;
        }
        return (T) context.getBean(beanId);
    }
}
