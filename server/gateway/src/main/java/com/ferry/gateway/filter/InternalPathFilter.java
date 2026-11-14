package com.ferry.gateway.filter;

import com.ferry.common.core.http.R;
import com.ferry.gateway.handler.GatewayExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 内部接口访问控制
 */
@Component
public class InternalPathFilter implements GlobalFilter, Ordered {

    /**
     * 仅允许服务间访问的路径前缀，不含服务名前缀
     */
    private static final String INTERNAL_PREFIX = "/internal";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        int index = path.indexOf('/', 1);
        // 去掉服务名前缀，得到服务内部的真实路径
        String innerPath = index > 0 ? path.substring(index) : path;
        // 容忍尾部斜杠，避免换个写法就绕过拦截
        if (innerPath.endsWith("/")) {
            innerPath = innerPath.substring(0, innerPath.length() - 1);
        }
        if (innerPath.equals(INTERNAL_PREFIX) || innerPath.startsWith(INTERNAL_PREFIX + "/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return GatewayExceptionHandler.write(exchange.getResponse(),
                    R.fail(HttpStatus.NOT_FOUND.value(), GatewayExceptionHandler.NOT_FOUND_MESSAGE));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 早于路由转发，避免请求已经打到下游服务
        return -1;
    }
}
