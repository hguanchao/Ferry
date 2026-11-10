package com.ferry.common.core.utils;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collection工具类
 */
public final class CollUtil extends CollectionUtils {

    private CollUtil() {
    }

    /**
     * 数组是否为空
     *
     * @param array 数组
     * @param <T>   数组元素类型
     * @return 是否为空
     */
    public static <T> boolean isEmpty(T[] array) {
        return ObjectUtils.isEmpty(array);
    }

    /**
     * 集合是否不为空
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 是否不为空
     */
    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    /**
     * Map是否不为空
     *
     * @param map Map
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 转为可变List
     *
     * @param elements 元素
     * @param <T>      元素类型
     * @return List
     */
    @SafeVarargs
    public static <T> List<T> asList(T... elements) {
        if (isEmpty(elements)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(elements));
    }

    /**
     * 转为Set
     *
     * @param elements 元素
     * @param <T>      元素类型
     * @return Set
     */
    @SafeVarargs
    public static <T> Set<T> asSet(T... elements) {
        return new HashSet<>(asList(elements));
    }

    /**
     * 转为Map，参数按 key, value 顺序成对传入
     *
     * @param kvs key value 数组
     * @param <K> key类型
     * @param <V> value类型
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> asMap(Object... kvs) {
        int kvLength = kvs.length;
        if (kvLength % IntConst.TWO != IntConst.ZERO) {
            return Collections.emptyMap();
        }
        Map<K, V> keyValueMap = new HashMap<>(kvLength);
        for (int i = 0; i < kvLength; i += IntConst.TWO) {
            keyValueMap.put((K) kvs[i], (V) kvs[i + IntConst.ONE]);
        }
        return keyValueMap;
    }

    /**
     * 逗号分隔字符串转为Long集合，空项按 0 处理
     *
     * @param str 被转换的值
     * @return List<Long>
     */
    public static List<Long> toLongList(String str) {
        if (StrUtil.isBlank(str)) {
            return Collections.emptyList();
        }
        String[] arr = str.split(StrConst.COMMA);
        List<Long> list = new ArrayList<>(arr.length);
        for (String item : arr) {
            String value = item.trim();
            list.add(value.isEmpty() ? 0L : Long.valueOf(value));
        }
        return list;
    }
}
