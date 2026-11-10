package com.ferry.common.core.utils;

import com.ferry.common.core.constant.StrConst;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 字符串工具类
 */
public final class StrUtil extends StringUtils {

    private StrUtil() {
    }

    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isBlank(CharSequence str) {
        return !hasText(str);
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotBlank(CharSequence str) {
        return hasText(str);
    }

    /**
     * 将对象转为String
     *
     * @param obj 对象
     * @return 字符串
     */
    public static String toStr(Object obj) {
        return toStr(obj, StrConst.EMPTY);
    }

    /**
     * 将对象转为String
     *
     * @param obj 对象
     * @param def 默认值
     * @return 字符串
     */
    public static String toStr(Object obj, String def) {
        return obj == null ? def : String.valueOf(obj);
    }

    /**
     * 空白时取默认值
     *
     * <p>与 {@link #toStr(Object, String)} 的区别：只在入参为空白（null、空串、纯空格）时兜底，
     * 非空白原样返回，适用于「没配就用默认值」的配置读取。
     *
     * @param str 字符串
     * @param def 默认值
     * @return 原字符串非空白时返回原值，否则返回默认值
     */
    public static String defaultIfBlank(CharSequence str, String def) {
        return isBlank(str) ? def : str.toString();
    }

    /**
     * 集合拼接转为字符串（逗号分隔）
     *
     * @param collection 集合
     * @return 拼接后的字符串
     */
    public static String join(Collection<?> collection) {
        return join(collection, StrConst.COMMA);
    }

    /**
     * 集合拼接转为字符串
     *
     * @param collection 集合
     * @param delimiter  分隔符
     * @return 拼接后的字符串
     */
    public static String join(Collection<?> collection, String delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return StrConst.EMPTY;
        }
        return collectionToDelimitedString(collection, delimiter);
    }

    /**
     * 格式化字符串，使用 {} 作为占位符
     *
     * @param str    字符串模板
     * @param params 参数列表
     * @return 格式化后的字符串
     */
    public static String format(String str, Object... params) {
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(str, params);
        return formattingTuple.getMessage();
    }

    /**
     * 是否以指定字符串开头，忽略大小写
     *
     * @param str    被检测字符串
     * @param prefix 开头字符串
     * @return 是否以指定字符串开头
     */
    public static boolean startWithIgnoreCase(CharSequence str, CharSequence prefix) {
        if (isBlank(str) || isBlank(prefix) || prefix.length() > str.length()) {
            return false;
        }
        return str.toString().regionMatches(true, 0, prefix.toString(), 0, prefix.length());
    }
}
