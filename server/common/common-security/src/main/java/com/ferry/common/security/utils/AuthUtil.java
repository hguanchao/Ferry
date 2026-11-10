package com.ferry.common.security.utils;

import com.ferry.common.cache.utils.RedisUtil;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.security.config.props.SecurityProperties;
import com.ferry.common.security.model.AuthInfo;
import com.ferry.common.security.model.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证工具类
 *
 * <p>公开方法统一排在类前部，按「会话 → 登录失败限制 → 当前用户」排列；
 * 内部实现统一放在类尾部，阅读时先看入口、再查细节。
 *
 * <p>令牌本体是字符串，走 {@link RedisUtil}；会话索引需要 Set 结构，直接使用模板。
 */
@Slf4j
public class AuthUtil {
    private static SecurityProperties properties;
    private static RedisTemplate<String, Object> redisTemplate;

    private static final String LOGIN_FAIL_CACHE_KEY = SysConst.AUTH_CACHE_PREFIX + "login:fail:";
    private static final String LOGIN_TOKEN_CACHE_KEY = SysConst.AUTH_CACHE_PREFIX + "login:token:";
    private static final String LOGIN_SESSION_CACHE_KEY = SysConst.AUTH_CACHE_PREFIX + "login:session:";

    /**
     * 构造器注入安全配置与模板，仅在首次赋值，避免多容器场景相互覆盖
     *
     * @param properties    安全配置
     * @param redisTemplate RedisTemplate，仅供会话索引使用
     */
    public AuthUtil(SecurityProperties properties, RedisTemplate<String, Object> redisTemplate) {
        if (AuthUtil.properties == null) {
            AuthUtil.properties = properties;
        }
        if (AuthUtil.redisTemplate == null) {
            AuthUtil.redisTemplate = redisTemplate;
        }
    }

    // ==================== 会话 ====================

    /**
     * 签发会话
     *
     * @param authUser 已通过凭据校验的用户
     * @return 认证信息，含访问令牌
     */
    public static AuthInfo login(AuthUser authUser) {
        AuthInfo authInfo = BeanUtil.copy(authUser, AuthInfo.class);
        authInfo.setUserId(authUser.getId());
        authUser.setPassword(null);
        authInfo.setPermissions(authUser.getPermissions());
        // 密码只在登录校验时使用，不随会话写入缓存，避免哈希在缓存中长期留存
        authInfo.setTokenHeader(properties.getTokenHeader());
        authInfo.setTokenPrefix(properties.getTokenPrefix());
        String token = authUser.getAccount() + ":" + UUID.randomUUID();
        authInfo.setAccessToken(CryptoUtil.aesEncrypt(token, properties.getSecretKey()));
        String key = LOGIN_TOKEN_CACHE_KEY + token;
        boolean isSuccess = RedisUtil.set(key, authUser, properties.getTokenTimeout());
        if (!isSuccess) {
            throw new AuthException("登录失败");
        }
        bindSession(authUser.getId(), token);
        return authInfo;
    }

    /**
     * 校验当前请求是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isLogin() {
        return isLogin(getRequest());
    }

    /**
     * 校验请求是否已登录，开启自动续签时顺带延长有效期
     *
     * @param request 请求体
     * @return 是否已登录
     */
    public static boolean isLogin(HttpServletRequest request) {
        String token = parseToken(request);
        if (StrUtil.isBlank(token)) {
            return false;
        }
        // 会话里存的是登录时的状态快照，禁用由 revokeAll 主动清理，这里的判断只作兜底
        AuthUser authUser = RedisUtil.get(LOGIN_TOKEN_CACHE_KEY + token);
        if (authUser == null || IntConst.ZERO.equals(authUser.getStatus())) {
            return false;
        }
        if (!properties.isAutoRenew()) {
            return true;
        }
        return renewToken(token);
    }

    /**
     * 登出：删除当前令牌并解除索引绑定
     *
     * @param request 请求体
     * @return 是否完成登出，令牌本就不存在时视为成功
     */
    public static boolean logout(HttpServletRequest request) {
        String token = parseToken(request);
        if (StrUtil.isBlank(token)) {
            return true;
        }
        String key = LOGIN_TOKEN_CACHE_KEY + token;
        // 先取用户再删：删除之后就无法再从令牌反查归属了
        AuthUser authUser = RedisUtil.get(key);
        boolean isDeleted = RedisUtil.del(key);
        if (isDeleted && authUser != null) {
            unbindSession(authUser.getId(), token);
        }
        return isDeleted;
    }

    /**
     * 撤销指定用户已签发的全部会话
     *
     * <p>改密、重置密码、禁用、删除用户之后旧令牌必须立即失效，不能等它自然过期。
     * 令牌无法按用户反查，因此依赖登录时维护的索引。
     *
     * @param userId 用户id
     */
    public static void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        Set<Object> sessions = redisTemplate.opsForSet().members(LOGIN_SESSION_CACHE_KEY + userId);
        if (CollUtil.isNotEmpty(sessions)) {
            String[] keys = sessions.stream()
                    .map(item -> LOGIN_TOKEN_CACHE_KEY + StrUtil.toStr(item)).toArray(String[]::new);
            // 删除失败时保留索引：令牌还在，索引没了就再也无法定位并撤销
            if (!RedisUtil.del(keys)) {
                return;
            }
        }
        redisTemplate.delete(LOGIN_SESSION_CACHE_KEY + userId);
    }

    // ==================== 登录失败限制 ====================

    /**
     * 检查账号是否已被锁定
     *
     * @param tenantId 租户id
     * @param account  账号
     * @throws AuthException 已达连续失败上限时抛出
     */
    public static void checkLoginLock(String tenantId, String account) {
        // 计数由 INCR 写入、读回时可能是整型或字符串，统一按文本转数值，避免反序列化类型不一致
        Object value = RedisUtil.get(loginFailKey(tenantId, account));
        long count = value == null ? 0L : Long.parseLong(StrUtil.toStr(value));
        if (count >= properties.getMaxFailCount()) {
            throw new AuthException("登录失败次数过多，请 " + (properties.getLockDuration() / 60) + " 分钟后再试");
        }
    }

    /**
     * 记录一次登录失败
     *
     * @param tenantId 租户id
     * @param account  账号
     */
    public static void loginLock(String tenantId, String account) {
        String key = loginFailKey(tenantId, account);
        Long count = RedisUtil.incr(key, 1);
        if (count == null) {
            return;
        }
        // 首次失败开始计时；命中阈值时再重置一次，让锁定从触发时刻算起。
        // 阈值之后不再延长，避免攻击者持续尝试就把账号永久锁死。
        long maxFailCount = properties.getMaxFailCount();
        if (count == 1L || count == maxFailCount) {
            RedisUtil.expire(key, properties.getLockDuration());
        }
    }

    /**
     * 登录成功后清除失败计数
     *
     * @param tenantId 租户id
     * @param account  账号
     */
    public static void loginUnlock(String tenantId, String account) {
        RedisUtil.del(loginFailKey(tenantId, account));
    }

    // ==================== 当前用户 ====================

    /**
     * 获取当前登录用户
     *
     * @return 登录用户，未登录时返回 null
     */
    public static AuthUser getUser() {
        return getUser(getRequest());
    }

    /**
     * 获取登录用户
     *
     * @param request 请求体
     * @return 登录用户，未登录时返回 null
     */
    public static AuthUser getUser(HttpServletRequest request) {
        String token = parseToken(request);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return RedisUtil.get(LOGIN_TOKEN_CACHE_KEY + token);
    }

    /**
     * 获取用户id
     *
     * @return 用户id，未登录时返回 null
     */
    public static Long getUserId() {
        AuthUser authUser = getUser();
        return authUser == null ? null : authUser.getId();
    }

    /**
     * 获取租户id
     *
     * @return 租户id，未登录时返回 null
     */
    public static String getTenantId() {
        AuthUser authUser = getUser();
        return authUser == null ? null : authUser.getTenantId();
    }

    /**
     * 获取用户账号
     *
     * @return 用户账号，未登录时返回 null
     */
    public static String getAccount() {
        AuthUser authUser = getUser();
        return authUser == null ? null : authUser.getAccount();
    }

    /**
     * 判断当前用户是否为超级管理员
     *
     * @return 是否超级管理员
     */
    public static boolean isSuper() {
        AuthUser authUser = getUser();
        return authUser != null && ObjUtil.equals(IntConst.ONE, authUser.getIsSuper());
    }

    /**
     * 获取当前请求
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Assert.notNull(requestAttributes, "The RequestAttributes is null");
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    // ==================== 内部实现 ====================

    /**
     * 续签令牌，同时延长会话索引，避免长期在线的会话先丢索引、之后无法撤销
     *
     * @param token 令牌
     * @return 是否续签成功
     */
    private static boolean renewToken(String token) {
        String key = LOGIN_TOKEN_CACHE_KEY + token;
        Long expiration = RedisUtil.getExpire(key);
        // 读取失败时返回 null，按未登录处理，避免拆箱空指针
        if (expiration == null || expiration <= 0) {
            return false;
        }
        if (expiration >= properties.getRenewThreshold()) {
            return true;
        }
        boolean isRenewed = RedisUtil.expire(key, properties.getRefreshTimeout());
        if (isRenewed) {
            AuthUser authUser = RedisUtil.get(key);
            if (authUser != null) {
                bindSession(authUser.getId(), token);
            }
        }
        return isRenewed;
    }

    /**
     * 解析令牌，去掉前缀并解密
     *
     * @param token 令牌字符串
     * @return 解密后的令牌，入参为空时返回 null
     */
    private static String parseToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        String tokenPrefix = properties.getTokenPrefix();
        if (StrUtil.startWithIgnoreCase(token, tokenPrefix)) {
            token = token.substring(tokenPrefix.length());
        }
        return CryptoUtil.aesDecrypt(token.trim(), properties.getSecretKey());
    }

    /**
     * 解析令牌
     *
     * @param request 请求体
     * @return 解密后的令牌
     */
    private static String parseToken(HttpServletRequest request) {
        return parseToken(request.getHeader(properties.getTokenHeader()));
    }

    /**
     * 记录用户与令牌的绑定
     *
     * <p>写失败只记日志：索引缺失会削弱撤销能力，但不应该让登录本身失败。
     * 索引有效期跟随续签上限，避免令牌早已过期、索引却长期残留。
     *
     * @param userId 用户id
     * @param token  令牌
     */
    private static void bindSession(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        try {
            String key = LOGIN_SESSION_CACHE_KEY + userId;
            redisTemplate.opsForSet().add(key, token);
            redisTemplate.expire(key, properties.getRefreshTimeout(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to bind session index, userId={}", userId, e);
        }
    }

    /**
     * 解除单个令牌的绑定
     *
     * @param userId 用户id
     * @param token  令牌
     */
    private static void unbindSession(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        try {
            redisTemplate.opsForSet().remove(LOGIN_SESSION_CACHE_KEY + userId, token);
        } catch (Exception e) {
            log.error("Failed to unbind session index, userId={}", userId, e);
        }
    }

    /**
     * 登录失败计数键
     *
     * <p>账号列大小写不敏感，若后续出现绕过锁定的情况，在此处对账号做归一化。
     *
     * @param tenantId 租户id
     * @param account  账号
     * @return 计数键
     */
    private static String loginFailKey(String tenantId, String account) {
        return LOGIN_FAIL_CACHE_KEY + StrUtil.toStr(tenantId) + ":" + StrUtil.toStr(account);
    }
}
