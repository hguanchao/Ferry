package com.ferry.common.core.utils;

import com.ferry.common.core.exception.UtilException;
import org.springframework.beans.BeanUtils;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bean对象工具类
 */
public final class BeanUtil {

    private BeanUtil() {
    }

    /**
     * BeanCopier 实例缓存，避免重复创建动态代理类
     */
    private static final Map<CopierIdentity, BeanCopier> BEAN_COPIERS = new ConcurrentHashMap<>();

    /**
     * 对象拷贝
     *
     * @param source 源Bean对象
     * @param target 目标Bean类
     * @return 目标对象
     */
    public static <T> T copy(Object source, Class<T> target) {
        if (source == null) {
            throw new UtilException("source must not be null");
        }
        T to = newInstance(target);
        create(source.getClass(), target).copy(source, to, null);
        return to;
    }

    /**
     * 集合拷贝
     *
     * @param collection 源Bean集合
     * @param target     目标Bean类
     * @param <T>        Bean类型
     * @return 目标集合
     */
    public static <T> List<T> copy(Collection<?> collection, Class<T> target) {
        if (CollUtil.isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream().map(source -> copy(source, target)).collect(Collectors.toList());
    }

    /**
     * 将对象转成Map
     *
     * @param bean 源对象
     * @return Map集合
     */
    public static Map<String, Object> toMap(Object bean) {
        return BeanMap.create(bean);
    }

    /**
     * 实例化对象
     *
     * @param clazz 类
     * @param <T>   泛型标记
     * @return 对象
     */
    private static <T> T newInstance(Class<T> clazz) {
        return (T) BeanUtils.instantiateClass(clazz);
    }

    /**
     * 获取BeanCopier（按源/目标类型缓存）
     *
     * @param source 源类型
     * @param target 目标类型
     * @return BeanCopier
     */
    private static BeanCopier create(Class<?> source, Class<?> target) {
        return BEAN_COPIERS.computeIfAbsent(new CopierIdentity(source, target),
                key -> BeanCopier.create(key.source(), key.target(), false));
    }

    /**
     * BeanCopier 缓存键
     *
     * @param source 源类型
     * @param target 目标类型
     */
    private record CopierIdentity(Class<?> source, Class<?> target) {
    }
}
