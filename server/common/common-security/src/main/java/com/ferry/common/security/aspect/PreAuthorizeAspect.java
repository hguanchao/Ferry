package com.ferry.common.security.aspect;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.http.HttpStatus;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.common.security.model.AuthPerm;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 接口权限校验切面
 *
 * <p>鉴权只对显式声明生效，不做默认拒绝：方法级注解优先于类级，两级都没有时直接放行。
 * 超级管理员最先短路，避免菜单漏配权限导致管理员被自己的注解拦在门外。
 */
@Aspect
public class PreAuthorizeAspect {

    /**
     * 校验调用方是否持有方法或所在类声明的权限
     *
     * @param point 连接点
     * @return 原方法的返回值
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(com.ferry.common.security.annotation.PreAuthorize)"
            + " || @within(com.ferry.common.security.annotation.PreAuthorize)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        PreAuthorize annotation = resolve(point);
        if (annotation == null) {
            return point.proceed();
        }
        AuthUser user = AuthUtil.getUser();
        if (user == null) {
            // 命中注解却取不到会话，说明接口落在免登录名单里，按未登录拒绝而不是按无权限拒绝
            throw new AuthException(HttpStatus.UN_AUTHORIZED, "请先登录");
        }
        if (!IntConst.ONE.equals(user.getIsSuper()) && !matches(user.getPermissions(), annotation.value())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "无操作权限");
        }
        return point.proceed();
    }

    /**
     * 解析生效的权限注解
     *
     * <p>切点虽然保证两级注解至少存在一个，但两级都可能存在，必须显式取方法级，
     * 不能靠切点绑定——绑定在两级同时存在时取到的是类级注解。
     *
     * @param point 连接点
     * @return 权限注解，两级都不存在时返回 null
     */
    private PreAuthorize resolve(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Class<?> targetClass = point.getTarget().getClass();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
        PreAuthorize onMethod = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
        return onMethod != null ? onMethod : AnnotatedElementUtils.findMergedAnnotation(targetClass, PreAuthorize.class);
    }

    /**
     * 判断持有的权限中是否存在满足所需标识的一项
     *
     * <p>标识按 {@code :} 分段比对，段数必须一致；任一侧的某一段为 {@code *} 时该段视为通过。
     * 两侧都允许通配，是为了让菜单上配置的模块级标识与注解上声明的动作级标识互相匹配；
     * 段数不做前缀放宽，否则少写一段的标识会意外扩大授权范围。
     *
     * @param permissions 用户持有的权限
     * @param required    所需的权限标识数组
     * @return 是否满足
     */
    private boolean matches(List<AuthPerm> permissions, String[] required) {
        if (CollUtil.isEmpty(permissions) || CollUtil.isEmpty(required)) {
            return false;
        }
        for (AuthPerm perm : permissions) {
            for (String item : required) {
                if (matches(perm.getPerms(), item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断单个持有的标识是否满足所需标识
     *
     * @param held     持有的权限标识
     * @param required 所需的权限标识
     * @return 是否满足
     */
    private boolean matches(String held, String required) {
        // 菜单表的权限标识可为空，先挡掉空值；空标识也必然匹配不上
        if (StrUtil.isBlank(held) || StrUtil.isBlank(required)) {
            return false;
        }
        // 末尾空段必须保留，否则 a:b: 会被截成两段，与 a:b 混为一谈
        String[] left = held.split(StrConst.COLON, -1);
        String[] right = required.split(StrConst.COLON, -1);
        if (left.length != right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            boolean hit = StrConst.ASTERISK.equals(left[i])
                    || StrConst.ASTERISK.equals(right[i])
                    || left[i].equals(right[i]);
            if (!hit) {
                return false;
            }
        }
        return true;
    }
}
