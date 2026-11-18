package com.ferry.common.cache.utils;

import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工具类
 *
 * <p>基于 Redisson 的 {@link RLock}：抢锁、续约、释放都由客户端完成，
 * 不自己拼 Lua，也不自己维护持有者标识——这些都是 Redisson 已经处理好的部分。
 *
 * <p>只提供「抢锁、续约、释放」三件事：排队等待、公平锁、联锁等高级用法需要时直接用
 * {@code RedissonClient} 拿对应的锁对象，不要在本类里堆策略。
 *
 * <p>客户端由 {@code RedisCacheAutoConfiguration} 导入本类时通过构造器写入静态字段，调用方以静态方式使用。
 */
@Slf4j
public class LockUtil {

    private static final long DEFAULT_WAIT_SECONDS = 0L;

    private static RedissonClient redissonClient;

    /**
     * 构造器注入客户端，仅在首次赋值，避免多容器场景相互覆盖
     *
     * @param client Redisson 客户端
     */
    public LockUtil(RedissonClient client) {
        if (LockUtil.redissonClient == null) {
            LockUtil.redissonClient = client;
        }
    }

    /**
     * 尝试获取锁，不等待
     *
     * @param key 锁键
     * @param ttl 锁有效期，传 null 表示启用看门狗自动续期（进程存活期间不释放）
     * @return true 抢到锁，false 已被占用或获取异常
     */
    public static boolean tryLock(String key, Duration ttl) {
        return tryLock(key, ttl, Duration.ZERO);
    }

    /**
     * 尝试获取锁
     *
     * @param key     锁键
     * @param ttl     锁有效期，传 null 表示启用看门狗自动续期
     * @param maxWait 最长等待时间，传 null 或不等待传 0
     * @return true 抢到锁，false 已占用或超时
     */
    public static boolean tryLock(String key, Duration ttl, Duration maxWait) {
        if (StrUtil.isBlank(key)) {
            log.warn("Lock skipped, key is blank");
            return false;
        }
        if (redissonClient == null) {
            log.error("Lock failed, redisson client not initialized, key={}", key);
            return false;
        }
        try {
            RLock lock = redissonClient.getLock(key);
            long waitSeconds = maxWait == null ? DEFAULT_WAIT_SECONDS : Math.max(maxWait.getSeconds(), 0L);
            if (ttl == null) {
                // 不指定 leaseTime 时 Redisson 启用看门狗，默认每 10 秒续期一次
                return lock.tryLock(waitSeconds, TimeUnit.SECONDS);
            }
            long leaseSeconds = ttl.getSeconds();
            if (leaseSeconds <= 0L) {
                log.warn("Lock skipped, ttl must be positive, key={}, ttl={}", key, ttl);
                return false;
            }
            return lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // 中断意味着调用方正在取消，保留中断标记并放弃抢锁
            Thread.currentThread().interrupt();
            log.warn("Lock interrupted, key={}", key);
            return false;
        } catch (Exception e) {
            log.error("Failed to acquire lock, key={}", key, e);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * <p>只释放当前线程持有的锁；不是持有者时 Redisson 抛异常，这里按释放失败处理。
     *
     * @param key 锁键
     * @return true 释放成功，false 非持有者或已释放
     */
    public static boolean unlock(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        if (redissonClient == null) {
            return false;
        }
        try {
            RLock lock = redissonClient.getLock(key);
            if (!lock.isHeldByCurrentThread()) {
                return false;
            }
            lock.unlock();
            return true;
        } catch (Exception e) {
            log.error("Failed to release lock, key={}", key, e);
            return false;
        }
    }
}
