package com.ferry.common.core.utils;

import org.springframework.util.ObjectUtils;

import java.util.Objects;

/**
 * Obj工具类
 */
public final class ObjUtil extends ObjectUtils {

    private ObjUtil() {
    }

    /**
     * 判断对象是否不为空
     *
     * @param obj 对象
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断两个对象是否相等
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否相等
     */
    public static boolean equals(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2);
    }
}
