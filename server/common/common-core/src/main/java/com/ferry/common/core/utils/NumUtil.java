package com.ferry.common.core.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 数值工具类
 */
public final class NumUtil {

    private NumUtil() {
    }

    /**
     * 获得指定长度的随机数（首位不为 0）
     *
     * @param length 长度，取值 1~9
     * @return Integer 随机数
     */
    public static Integer randomInt(int length) {
        if (length < 1 || length > 9) {
            throw new IllegalArgumentException("随机数长度必须在 1~9 之间");
        }
        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 将对象转为 Long
     *
     * <p>空白（null、空串、纯空格）返回 null，因为空值本身常是合法语义，例如超管不挂部门；
     * 非数字格式直接抛异常，不把脏数据当成空值静默吞掉。
     *
     * @param obj 待转换对象
     * @return 转换结果，空白时为 null
     * @throws IllegalArgumentException 非数字格式时抛出
     */
    public static Long toLong(Object obj) {
        String str = StrUtil.toStr(obj).trim();
        if (StrUtil.isBlank(str)) {
            return null;
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(StrUtil.format("无法将 [{}] 转为 Long", str), e);
        }
    }

    /**
     * 将对象转为 Long，空白或非数字格式时取默认值
     *
     * <p>只有明确接受兜底语义时才用本方法，否则用 {@link #toLong(Object)} 暴露非法数据。
     *
     * @param obj 待转换对象
     * @param def 默认值
     * @return 转换结果，空白或非数字格式时为默认值
     */
    public static Long toLong(Object obj, Long def) {
        String str = StrUtil.toStr(obj).trim();
        if (StrUtil.isBlank(str)) {
            return def;
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 将对象转为 Integer
     *
     * <p>空白（null、空串、纯空格）返回 null；非数字格式或超出 int 范围时抛异常。
     *
     * @param obj 待转换对象
     * @return 转换结果，空白时为 null
     * @throws IllegalArgumentException 非数字格式或超出 int 范围时抛出
     */
    public static Integer toInt(Object obj) {
        String str = StrUtil.toStr(obj).trim();
        if (StrUtil.isBlank(str)) {
            return null;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(StrUtil.format("无法将 [{}] 转为 Integer", str), e);
        }
    }

    /**
     * 将对象转为 Integer，空白或非数字格式时取默认值
     *
     * <p>只有明确接受兜底语义时才用本方法，否则用 {@link #toInt(Object)} 暴露非法数据。
     *
     * @param obj 待转换对象
     * @param def 默认值
     * @return 转换结果，空白或非数字格式时为默认值
     */
    public static Integer toInt(Object obj, Integer def) {
        String str = StrUtil.toStr(obj).trim();
        if (StrUtil.isBlank(str)) {
            return def;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
