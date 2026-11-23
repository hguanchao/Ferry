package com.ferry.common.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 正则校验工具类
 *
 * <p>固定规则在这里定义成方法；零散规则用 {@link #matches(String, CharSequence)} 传入正则，
 * 避免在业务类里散落 {@code Pattern} 常量。
 */
public final class RegexUtil {

    /**
     * 手机号：1开头，第二位3-9，共11位
     */
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 整数：可带正负号，不含小数点
     */
    private static final Pattern NUMERIC = Pattern.compile("^[+-]?\\d+$");

    /**
     * 小数：可带正负号，必须含小数点
     */
    private static final Pattern DECIMAL = Pattern.compile("^[+-]?\\d+\\.\\d+$");

    /**
     * 邮箱：常规 本地部分@域名.顶级域 格式
     */
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * URL：http(s) 协议，主机部分不含空白
     */
    private static final Pattern URL = Pattern.compile("^https?://[^\\s]+$");

    /**
     * IPv4：四段 0-255
     */
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$");

    /**
     * UUID：8-4-4-4-12 十六进制
     */
    private static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * 零散正则的编译缓存，避免同一规则重复编译
     */
    private static final Map<String, Pattern> CACHE = new ConcurrentHashMap<>();

    private RegexUtil() {
    }

    /**
     * 校验手机号
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isPhone(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return PHONE.matcher(value).matches();
    }

    /**
     * 校验整数（可带正负号，不含小数点）
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isNumeric(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return NUMERIC.matcher(value).matches();
    }

    /**
     * 校验小数（可带正负号，必须含小数点）
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return DECIMAL.matcher(value).matches();
    }

    /**
     * 校验邮箱
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isEmail(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return EMAIL.matcher(value).matches();
    }

    /**
     * 校验 http/https 地址
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isUrl(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return URL.matcher(value).matches();
    }

    /**
     * 校验 IPv4 地址
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isIpv4(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return IPV4.matcher(value).matches();
    }

    /**
     * 校验 UUID
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isUuid(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return UUID.matcher(value).matches();
    }

    /**
     * 按指定正则校验
     *
     * <p>只用于零散规则；同一正则反复使用时应当在工具类里定义成方法，别每次都走缓存查找。
     *
     * @param regex 正则表达式
     * @param value 待校验值
     * @return 是否匹配，任一入参为空时返回 false
     */
    public static boolean matches(String regex, CharSequence value) {
        if (StrUtil.isBlank(regex) || StrUtil.isBlank(value)) {
            return false;
        }
        return CACHE.computeIfAbsent(regex, Pattern::compile).matcher(value).matches();
    }

    /**
     * 按指定正则替换所有命中片段
     *
     * <p>用于脱敏等"匹配并改写"场景；替换串支持 {@code $1} 等分组引用。
     *
     * @param regex       正则表达式
     * @param value       待处理文本
     * @param replacement 替换串
     * @return 替换结果；正则或文本为空时原样返回文本
     */
    public static String replaceAll(String regex, CharSequence value, String replacement) {
        if (StrUtil.isBlank(regex) || StrUtil.isBlank(value)) {
            return value == null ? null : value.toString();
        }
        return CACHE.computeIfAbsent(regex, Pattern::compile).matcher(value).replaceAll(replacement);
    }
}
