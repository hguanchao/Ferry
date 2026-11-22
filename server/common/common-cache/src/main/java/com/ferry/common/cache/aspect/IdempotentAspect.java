package com.ferry.common.cache.aspect;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.ferry.common.cache.annotation.Idempotent;
import com.ferry.common.cache.config.props.IdempotentProperties;
import com.ferry.common.cache.utils.RedisUtil;
import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Redis 幂等切面：SET NX PX 抢占幂等键，重复调用直接拒绝。
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final IdempotentProperties properties;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        if (properties == null || !properties.isEnabled()) {
            return joinPoint.proceed();
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String key = buildKey(method, joinPoint.getArgs(), idempotent);
        int ttl = idempotent.ttl() > 0 ? idempotent.ttl() : properties.getDefaultTtlSeconds();
        Boolean acquired = RedisUtil.trySetNx(key, "1", ttl, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            throw new ServiceException(idempotent.message());
        }
        if (acquired == null) {
            if (!idempotent.failOpen()) {
                throw new ServiceException("幂等校验失败，请稍后重试");
            }
            log.warn("Redis unavailable, idempotent check skipped: key={}", key);
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            if (idempotent.releaseOnError() && Boolean.TRUE.equals(acquired)) {
                RedisUtil.del(key);
            }
            throw e;
        }
    }

    private String buildKey(Method method, Object[] args, Idempotent idempotent) {
        String raw = evaluate(idempotent.key(), method, args);
        if (StrUtil.isBlank(raw)) {
            try {
                raw = JsonUtil.toJson(args);
            } catch (Exception e) {
                raw = String.valueOf(java.util.Arrays.deepToString(args));
            }
        }
        String logical = method.getDeclaringClass().getSimpleName() + "." + method.getName()
                + ":" + (raw == null ? "" : raw);
        return SysConst.IDEMPOTENT_CACHE_PREFIX + CryptoUtil.sha256Hex(logical);
    }

    private String evaluate(String expression, Method method, Object[] args) {
        if (StrUtil.isBlank(expression)) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (args.length == 1) {
                // 常见控制器入参：@RequestBody DTO，兼容 #body.xxx
                context.setVariable("body", args[0]);
                context.setVariable("dto", args[0]);
            }
            context.setVariable("args", args);
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
            String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
            if (names != null) {
                for (int i = 0; i < names.length && i < args.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            Object value = EXPRESSION_PARSER.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("idempotent key expression evaluate failed: expression={}", expression, e);
            return null;
        }
    }
}
