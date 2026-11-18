package com.ferry.gateway.filter;

import com.ferry.common.core.constant.SysConst;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 开放 API 鉴权与限流过滤器
 *
 * <p>只作用于 /ferry-devops/open/**（网关路由前缀）。Token 的真实校验在 devops 服务的
 * internal 接口完成，这里只做「必填 + 每分钟 100 次」的粗粒度前置拦截，避免无效请求打到下游。
 * 计数为本实例内存计数，多实例部署时由网关前置的限流组件收口，符合本期最小实现口径。
 */
@Component
public class OpenApiAuthFilter implements GlobalFilter, Ordered {
    private static final String OPEN_PREFIX = "/ferry-devops/open/";
    private static final String TOKEN_HEADER = "X-Api-Token";
    private static final String APP_ID_HEADER = "X-Open-App-Id";
    private static final String TENANT_HEADER = SysConst.TENANT_ID;
    private static final int LIMIT_PER_MINUTE = 100;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith(OPEN_PREFIX)) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String key = token;
        long minute = System.currentTimeMillis() / 60_000;
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());
        if (counter.minute != minute) {
            synchronized (counter) {
                if (counter.minute != minute) {
                    counter.minute = minute;
                    counter.count.set(0);
                }
            }
        }
        if (counter.count.incrementAndGet() > LIMIT_PER_MINUTE) {
            // 限流响应不带业务体，直接 429，由前端统一提示
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        // Token 对应的 appId/tenantId 由下游根据 token 解析，这里只保证有 Token 头
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 早于业务路由转发
        return -2;
    }

    /** 每分钟窗口计数 */
    private static class WindowCounter {
        private volatile long minute = System.currentTimeMillis() / 60_000;
        private final AtomicInteger count = new AtomicInteger();
    }
}
