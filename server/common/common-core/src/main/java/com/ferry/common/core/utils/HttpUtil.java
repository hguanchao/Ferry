package com.ferry.common.core.utils;

import com.ferry.common.core.exception.UtilException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求工具类
 *
 * <p>基于 JDK HttpClient 的同步阻塞请求，面向调用第三方开放接口的轻量场景；
 * 响应体按文本接收，JSON 解析交给 {@link JsonUtil}。
 * HTTP 层失败（连接失败、超时、被中断）记 error 日志后抛 {@link UtilException}；
 * 响应状态码不做判断，交由调用方按业务语义处理。
 *
 * <p>第三方调用不经过控制器的请求日志切面，完成与失败都在这里记录；
 * 地址可能带令牌类查询参数，落日志前先打码，请求体与响应体一律不打印。
 */
@Slf4j
public final class HttpUtil {

    /**
     * HTTP GET 方法名
     */
    private static final String GET = "GET";

    /**
     * HTTP POST 方法名
     */
    private static final String POST = "POST";

    /**
     * 默认连接与请求超时：面向表单交互等短等待场景
     */
    private static final Duration TIMEOUT = Duration.ofMillis(5000);

    /**
     * HttpClient 不可变且线程安全，全进程共用一个实例
     */
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private HttpUtil() {
    }

    /**
     * 发起 GET 请求
     *
     * @param url 完整请求地址
     * @param headers 请求头，可为 null
     * @return 响应
     */
    public static Response get(String url, Map<String, String> headers) {
        return request(GET, url, null, headers);
    }

    /**
     * 发起 POST 请求
     *
     * <p>请求体非空时默认按 application/json 发送，headers 里的同名头可覆盖。
     *
     * @param url 完整请求地址
     * @param body 请求体文本，null 或空串按无请求体发送
     * @param headers 请求头，可为 null
     * @return 响应
     */
    public static Response post(String url, String body, Map<String, String> headers) {
        // 调用方传入的可能是 Map.of 等不可变 Map，合并到本地 Map 再补默认值，避免改动入参
        Map<String, String> merged = new HashMap<>();
        if (CollUtil.isNotEmpty(headers)) {
            merged.putAll(headers);
        }
        if (StrUtil.isNotBlank(body)) {
            merged.putIfAbsent("Content-Type", "application/json");
        }
        return request(POST, url, body, merged);
    }

    /**
     * 发起 HTTP 请求
     *
     * @param method 请求方法，如 GET / POST / PUT / DELETE
     * @param url 完整请求地址
     * @param body 请求体文本，GET 等无请求体方法传 null
     * @param headers 请求头，可为 null
     * @return 响应
     */
    public static Response request(String method, String url, String body, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT)
                .setHeader("Accept", "application/json");
        if (CollUtil.isNotEmpty(headers)) {
            headers.forEach(builder::setHeader);
        }
        if (StrUtil.isNotBlank(body)) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            long start = System.nanoTime();
            HttpResponse<String> resp = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long end = System.nanoTime();
            log.info("HTTP {} {} -> {} ({} ms)", method, url, resp.statusCode(), (end - start) / 1_000_000);
            return new Response(resp.statusCode(), resp.body());
        } catch (IOException e) {
            // 连接超时、读取超时与域名不可达在此归并，具体话术由调用方按业务决定
            log.error("HTTP {} {} 失败", method, url, e);
            throw new UtilException("HTTP 请求失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HTTP {} {} 被中断", method, url, e);
            throw new UtilException("HTTP 请求被中断", e);
        }
    }

    /**
     * HTTP 响应
     *
     * @param status 状态码
     * @param body 响应体文本
     */
    public record Response(int status, String body) {

        /**
         * 判断是否 2xx 成功
         *
         * @return 是否成功
         */
        public boolean isOk() {
            return status >= 200 && status < 300;
        }
    }
}
