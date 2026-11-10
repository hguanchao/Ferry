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
     * 仓库地址：http(s) 协议且以 .git 结尾
     */
    private static final Pattern REPO_URL = Pattern.compile("^https?://\\S+\\.git$");

    /**
     * 提交 id：7-40 位十六进制
     */
    private static final Pattern COMMIT_ID = Pattern.compile("^[0-9a-fA-F]{7,40}$");

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
     * 校验仓库地址
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isRepoUrl(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return REPO_URL.matcher(value).matches();
    }

    /**
     * 校验提交 id
     *
     * @param value 待校验值
     * @return 是否匹配
     */
    public static boolean isCommitId(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return COMMIT_ID.matcher(value).matches();
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
}
