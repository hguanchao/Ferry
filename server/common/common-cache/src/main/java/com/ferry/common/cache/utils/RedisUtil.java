package com.ferry.common.cache.utils;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类。
 *
 * <p>只保留字符串读写、过期控制、删除与计数这几类高频操作；Hash、Set、List 等结构直接用
 * {@link RedisTemplate} 操作，避免工具类无限膨胀。
 *
 * <p>模板由 {@code RedisCacheAutoConfiguration} 导入本类时通过构造器写入静态字段，业务侧仍以静态方式调用。
 *
 * <p>键参数标 {@link NonNull}，用于让 IDE 在编码阶段提示可能传入的空值；运行期不做校验。
 */
@Slf4j
@SuppressWarnings("unchecked")
public class RedisUtil {

    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造器注入模板，仅在首次赋值，避免多容器场景相互覆盖
     *
     * @param redisTemplate RedisTemplate
     */
    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        if (RedisUtil.redisTemplate == null) {
            RedisUtil.redisTemplate = redisTemplate;
        }
    }

    /**
     * 读取缓存
     *
     * @param key 键
     * @param <T> 值类型
     * @return 值，不存在或异常时为 null
     */
    public static <T> T get(@NonNull String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to read cache entry, key={}", key, e);
            return null;
        }
    }

    /**
     * 写入缓存，不设置过期时间
     *
     * @param key   键
     * @param value 值
     * @return 是否写入成功
     */
    public static Boolean set(@NonNull String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Failed to write cache entry, key={}", key, e);
            return false;
        }
    }

    /**
     * 写入缓存并设置过期时间
     *
     * @param key   键
     * @param value 值
     * @param time  过期时间(秒)，小于等于 0 表示不过期
     * @return 是否写入成功
     */
    public static Boolean set(@NonNull String key, Object value, long time) {
        return set(key, value, time, TimeUnit.SECONDS);
    }

    /**
     * 写入缓存并设置过期时间
     *
     * @param key      键
     * @param value    值
     * @param time     过期时间，小于等于 0 表示不过期
     * @param timeUnit 时间单位
     * @return 是否写入成功
     */
    public static Boolean set(@NonNull String key, Object value, long time, TimeUnit timeUnit) {
        // Redis 不接受过期时间为 0 的写入，这里退化为不过期
        if (time <= 0L) {
            return set(key, value);
        }
        try {
            redisTemplate.opsForValue().set(key, value, time, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Redis SET failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 仅当键不存在时写入，可用于防重复提交
     *
     * @param key   键
     * @param value 值
     * @return true 写入成功（键原本不存在），false 键已存在或写入异常
     */
    public static Boolean setNx(@NonNull String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
        } catch (Exception e) {
            log.error("Failed to write cache entry if absent, key={}", key, e);
            return false;
        }
    }

    /**
     * 仅当键不存在时写入并设置过期时间
     *
     * @param key      键
     * @param value    值
     * @param time     过期时间，小于等于 0 表示不过期
     * @param timeUnit 时间单位
     * @return true 写入成功（键原本不存在），false 键已存在或写入异常
     */
    public static Boolean setNx(@NonNull String key, Object value, long time, TimeUnit timeUnit) {
        if (time <= 0L) {
            return setNx(key, value);
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, time, timeUnit));
        } catch (Exception e) {
            log.error("Redis SETNX failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 自增，键不存在时按 0 起始
     *
     * @param key   键
     * @param delta 增量，需大于 0
     * @return 自增后的值，异常时为 null
     */
    public static Long incr(@NonNull String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Failed to increment counter, key={}, delta={}", key, delta, e);
            return null;
        }
    }

    /**
     * 自减
     *
     * @param key   键
     * @param delta 减量，需大于 0
     * @return 自减后的值，异常时为 null
     */
    public static Long decr(@NonNull String key, long delta) {
        return incr(key, -delta);
    }

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return true 存在 false 不存在或异常
     */
    public static boolean hasKey(@NonNull String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check cache key existence, key={}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key  键
     * @param time 过期时间(秒)，需大于 0
     * @return 是否设置成功
     */
    public static Boolean expire(@NonNull String key, long time) {
        return expire(key, time, TimeUnit.SECONDS);
    }

    /**
     * 设置过期时间
     *
     * @param key      键
     * @param time     过期时间，需大于 0
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    public static Boolean expire(@NonNull String key, long time, TimeUnit timeUnit) {
        if (time <= 0L) {
            // Redis 中 EXPIRE 0 会直接删除键，与"永久有效"的语义相反，这里直接拒绝
            log.warn("Expiration skipped, ttl must be positive, key={}, ttl={}", key, time);
            return false;
        }
        try {
            return redisTemplate.expire(key, time, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set expiration, key={}, ttl={}, unit={}", key, time, timeUnit, e);
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     *
     * @param key 键
     * @return 剩余秒数，-1 永久有效，-2 键不存在，异常时为 null
     */
    public static Long getExpire(@NonNull String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to read expiration, key={}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param keys 键，可传一个或多个
     * @return 是否至少删除成功一个键
     */
    public static Boolean del(@NonNull String... keys) {
        if (keys.length == 0) {
            return false;
        }
        try {
            if (keys.length == 1) {
                return redisTemplate.delete(keys[0]);
            }
            Long count = redisTemplate.delete(Arrays.asList(keys));
            return count > 0L;
        } catch (Exception e) {
            log.error("Failed to delete cache keys, keys={}", Arrays.toString(keys), e);
            return false;
        }
    }

    /**
     * 按表达式匹配键，仅用于小数据量清理，禁止在大库上执行
     *
     * @param pattern 表达式，如 ferry:login:token:*
     * @param <T>     键类型
     * @return 匹配到的键集合
     */
    public static <T> Set<T> keys(@NonNull String pattern) {
        try {
            return (Set<T>) redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Failed to match cache keys, pattern={}", pattern, e);
            return null;
        }
    }
}
