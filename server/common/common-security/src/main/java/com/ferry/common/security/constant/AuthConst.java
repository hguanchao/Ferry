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
            "/internal/user/auth",
            "/tenant/list",
            "/actuator/health",
            "/v3/api-docs/**",
            "/error/**"
    );
}
