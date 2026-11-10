package com.ferry.common.core.utils;

import com.ferry.common.core.constant.IntConst;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 时间日期工具类
 */
public final class DateUtil {

    public static final String PURE_DATE_PATTERN = "yyyyMMdd";
    public static final String PURE_DATETIME_PATTERN = "yyyyMMddHHmmss";
    public static final String NORM_DATE_PATTERN = "yyyy-MM-dd";
    public static final String NORM_TIME_PATTERN = "HH:mm:ss";
    public static final String NORM_DATETIME_MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
    public static final String NORM_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter PURE_DATE_FORMATTER = DateTimeFormatter.ofPattern(PURE_DATE_PATTERN);
    public static final DateTimeFormatter NORM_DATE_FORMATTER = DateTimeFormatter.ofPattern(NORM_DATE_PATTERN);
    public static final DateTimeFormatter NORM_TIME_FORMATTER = DateTimeFormatter.ofPattern(NORM_TIME_PATTERN);
    public static final DateTimeFormatter NORM_DATETIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern(NORM_DATETIME_MINUTE_PATTERN);
    public static final DateTimeFormatter NORM_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(NORM_DATETIME_PATTERN);
    public static final DateTimeFormatter PURE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PURE_DATETIME_PATTERN);

    /**
     * 自定义格式的 Formatter 缓存，避免每次格式化都创建实例
     */
    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private DateUtil() {
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static Date now() {
        return new Date();
    }

    /**
     * Long类型时间转为{@link Date}<br>
     * 只支持毫秒级别时间戳，如果需要秒级别时间戳，请自行×1000
     *
     * @param date Long类型Date（Unix时间戳）
     * @return 时间对象
     */
    public static Date date(long date) {
        return new Date(date);
    }

    /**
     * 格式化日期，默认 yyyy-MM-dd HH:mm:ss
     *
     * @param date 日期
     * @return 格式化后的日期字符串
     */
    public static String format(Date date) {
        if (null == date) {
            return null;
        }
        return format(date.toInstant(), NORM_DATETIME_FORMATTER);
    }

    /**
     * 格式化日期
     *
     * @param date   日期
     * @param format 日期格式
     * @return 格式化后的日期字符串
     */
    public static String format(Date date, String format) {
        if (null == format || null == date) {
            return null;
        }
        return format(date.toInstant(), formatter(format));
    }

    /**
     * 格式化日期
     *
     * @param date   日期
     * @param format 日期格式化器
     * @return 格式化后的日期字符串
     */
    public static String format(Date date, DateTimeFormatter format) {
        if (null == format || null == date) {
            return null;
        }
        return format(date.toInstant(), format);
    }

    /**
     * 格式化时间
     *
     * @param time      时间
     * @param formatter 日期格式化器
     * @return 格式化后的时间字符串
     */
    public static String format(TemporalAccessor time, DateTimeFormatter formatter) {
        if (null == time) {
            return null;
        }
        if (time instanceof Month) {
            return time.toString();
        }
        if (null == formatter) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        }
        try {
            return formatter.format(time);
        } catch (UnsupportedTemporalTypeException e) {
            return switch (time) {
                case LocalDate localDate when e.getMessage().contains("HourOfDay") ->
                    // 传入 LocalDate，但格式包含时间部分，补 00:00:00
                        formatter.format(localDate.atStartOfDay());
                case LocalTime localTime when e.getMessage().contains("YearOfEra") ->
                    // 传入 LocalTime，但格式包含日期部分，补当天日期
                        formatter.format(localTime.atDate(LocalDate.now()));
                case Instant instant ->
                    // 时间戳没有时区信息，赋予默认时区
                        formatter.format(instant.atZone(ZoneId.systemDefault()));
                default -> throw e;
            };
        }
    }

    /**
     * 时间差（秒）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒
     */
    public static Long between(Date start, Date end) {
        return (end.getTime() - start.getTime()) / IntConst.ONE_THOUSAND;
    }

    /**
     * 时间差
     *
     * @param start    开始时间
     * @param end      结束时间
     * @param timeUnit 时间单位
     * @return 根据时间单位返回
     */
    public static Long between(LocalDate start, LocalDate end, TimeUnit timeUnit) {
        return between(start.atStartOfDay(), end.atStartOfDay(), timeUnit);
    }

    /**
     * 时间差
     *
     * @param start    开始时间
     * @param end      结束时间
     * @param timeUnit 时间单位
     * @return 根据时间单位返回
     */
    public static Long between(LocalDateTime start, LocalDateTime end, TimeUnit timeUnit) {
        Duration duration = Duration.between(start, end);
        return switch (timeUnit) {
            case MILLISECONDS -> duration.toMillis();
            case MINUTES -> duration.toMinutes();
            case HOURS -> duration.toHours();
            case DAYS -> duration.toDays();
            default -> duration.getSeconds();
        };
    }

    /**
     * 按格式获取 Formatter（带缓存）
     *
     * @param pattern 日期格式
     * @return DateTimeFormatter
     */
    private static DateTimeFormatter formatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }
}
