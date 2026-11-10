package com.ferry.common.core.utils;

import com.ferry.common.core.exception.UtilException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP 请求工具测试
 *
 * <p>用 JDK 自带的 HttpServer 起本地端点，验证方法封装、请求头与请求体的携带、
 * 非 2xx 状态的返回语义以及网络失败路径，不依赖外部网络。
 */
class HttpUtilTest {

    private HttpServer server;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getShouldReturnStatusAndBody() {
        server.createContext("/ok", exchange -> {
            byte[] bytes = "{\"name\":\"main\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        HttpUtil.Response resp = HttpUtil.get(base + "/ok", null);
        assertTrue(resp.isOk());
        assertEquals(200, resp.status());
        assertEquals("{\"name\":\"main\"}", resp.body());
    }

    @Test
    void getShouldCarryHeaders() {
        AtomicReference<String> token = new AtomicReference<>();
        server.createContext("/header", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        HttpUtil.get(base + "/header", Map.of("PRIVATE-TOKEN", "glpat-x"));
        assertEquals("glpat-x", token.get());
    }

    @Test
    void non2xxShouldBeReturnedNotThrown() {
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        HttpUtil.Response resp = HttpUtil.get(base + "/missing", null);
        assertFalse(resp.isOk());
        assertEquals(404, resp.status());
    }

    @Test
    void postShouldSendBodyWithJsonContentType() {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> bodyText = new AtomicReference<>();
        server.createContext("/echo", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            bodyText.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        HttpUtil.Response resp = HttpUtil.post(base + "/echo", "{\"a\":1}", null);
        assertTrue(resp.isOk());
        assertEquals("application/json", contentType.get());
        assertEquals("{\"a\":1}", bodyText.get());
    }

    @Test
    void callerHeaderShouldOverrideDefault() {
        AtomicReference<String> contentType = new AtomicReference<>();
        server.createContext("/cover", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        HttpUtil.post(base + "/cover", "{\"a\":1}", Map.of("Content-Type", "text/plain"));
        assertEquals("text/plain", contentType.get());
    }

    @Test
    void unreachableHostShouldThrow() {
        // 端口 1 无监听，连接必然被拒绝
        assertThrows(UtilException.class, () -> HttpUtil.get("http://127.0.0.1:1/x", null));
    }
}
