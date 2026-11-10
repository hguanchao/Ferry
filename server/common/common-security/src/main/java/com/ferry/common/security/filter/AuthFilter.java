package com.ferry.common.security.filter;

import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.security.config.props.SecurityProperties;
import com.ferry.common.security.utils.AuthUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ferry.common.core.http.HttpStatus.UN_AUTHORIZED;

/**
 * 鉴权认证
 */
public class AuthFilter implements Filter {
    private final SecurityProperties properties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 构造鉴权过滤器
     *
     * @param properties 安全配置
     */
    public AuthFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (excludePath(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader(properties.getTokenHeader());
        if (StrUtil.isBlank(token)) {
            write(response, R.fail(UN_AUTHORIZED, "缺失访问令牌，请重新登录"));
            return;
        }
        try {
            boolean isLogin = AuthUtil.isLogin(request);
            if (!isLogin) {
                write(response, R.fail(UN_AUTHORIZED, "访问令牌已失效，请重新登录"));
                return;
            }
        } catch (Exception e) {
            write(response, R.fail(UN_AUTHORIZED, "认证失败，请重新登录"));
            return;
        }
        chain.doFilter(request, response);
    }


    /**
     * 排除不需要鉴权认证的请求路径
     *
     * @param path 请求路径
     * @return 不需要鉴权认证的请求路径
     */
    private boolean excludePath(String path) {
        return properties.getExcludePath().stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    private void write(HttpServletResponse response, R<?> result) throws IOException {
        response.setStatus(result.getCode());
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        response.getWriter().print(JsonUtil.toJson(result));
    }
}
