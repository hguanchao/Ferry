package com.ferry.common.security.constant;

import com.ferry.common.core.utils.CollUtil;

import java.util.Set;

/**
 * 认证常量
 */
public interface AuthConst {

    /**
     * 客户端id
     */
    String CLIENT_ID = "Client-Id";

    /**
     * 客户端Token
     */
    String CLIENT_TOKEN = "Client-Token";

    /**
     * 令牌秘钥
     */
    String SECRET = "e1901e9cd3bedc5ab93271159e45b17d1db2e74c";

    /**
     * 默认鉴权排除的请求路径
     */
    Set<String> DEFAULT_EXCLUDE_PATH = CollUtil.asSet(
            "/login",
            "/**/captcha",
            // 内部接口由网关挡住外部访问，这里放行是因为服务间调用不带令牌。
            // 登记的是具体路径而不是 /internal/**：新增内部接口要显式加一行，避免一条通配把业务接口也放进来。
            // Jenkins 结果回传走公开回调：不携带登录令牌，由回调控制器自行做签名校验。
            // 网关侧 /internal/** 仍对外部关闭，回调路径不受影响。
            "/callback/**",
            "/internal/user/auth",
            // 交付域内部接口：服务间调用不带令牌，由网关 InternalPathFilter 挡住外部访问。
            "/internal/app/options",
            "/internal/app/owner",
            "/internal/environment/options",
            // 登录页选择租户用，此时还没有会话，标注权限会直接 401。
            "/tenant/list",
            "/actuator/health",
            "/v3/api-docs/**",
            "/error/**"
    );
}
