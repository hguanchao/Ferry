package com.ferry.gateway.handler;

import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.JsonUtil;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关异常全局处理器
 */
@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    /**
     * 未找到路由时的提示语，内部接口拦截也复用，避免外部探测出路径存在
     */
    public static final String NOT_FOUND_MESSAGE = "404 请求未找到";

    /**
     * 统一处理网关异常
     *
     * @param exchange 当前请求响应上下文
     * @param ex       异常对象，由 WebFlux 保证非空
     * @return 响应写出结果
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        if (ex instanceof ResponseStatusException) {
            response.setStatusCode(((ResponseStatusException) ex).getStatusCode());
        }
        HttpStatusCode statusCode = response.getStatusCode();
        String message;
        if (statusCode != null && statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            message = NOT_FOUND_MESSAGE;
        } else if (statusCode != null && statusCode.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
            message = "服务器维护中...";
        } else {
            message = ex.getMessage();
        }
        int code = statusCode == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : statusCode.value();
        return write(response, R.fail(code, message));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 写出 JSON 响应
     *
     * @param message  响应消息
     * @param response HTTP 响应
     * @return 写出结果
     */
    public static Mono<Void> write(ServerHttpResponse response, Object message) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(message));
        return response.writeWith(Mono.just(dataBuffer));
    }
}
