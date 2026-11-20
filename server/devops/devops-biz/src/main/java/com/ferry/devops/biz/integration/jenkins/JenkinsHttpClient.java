package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Jenkins REST 调用：Cookie 会话、CSRF crumb、表单触发与 XML 建 Job。
 *
 * <p>使用独立 HttpClient 并保留 Cookie，保证「取 crumb → 建 Job/触发构建」在同一会话中完成；
 * 请求失败按是否可能已送达 Jenkins 归类为明确失败或结果待确认，交给调用方决定运行状态。
 */
@Slf4j
final class JenkinsHttpClient {

    /**
     * 单次请求超时：Jenkins API 为短请求，避免阻塞触发接口
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient client;
    private final String baseUrl;
    private final String authorization;
    private volatile String crumb;
    private volatile String crumbField;
    private volatile boolean crumbResolved;

    JenkinsHttpClient(String baseUrl, String user, String token) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.authorization = StrUtil.isNotBlank(user) && StrUtil.isNotBlank(token)
                ? "Basic " + Base64.getEncoder().encodeToString(
                (user + ":" + token).getBytes(StandardCharsets.UTF_8))
                : null;
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    HttpResult get(String path) {
        return send("GET", path, null, null);
    }

    HttpResult postForm(String path, Map<String, String> params) {
        String body = params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return post(path, body, "application/x-www-form-urlencoded; charset=UTF-8");
    }

    HttpResult postXml(String path, String xml) {
        return post(path, xml, "application/xml; charset=UTF-8");
    }

    private HttpResult post(String path, String body, String contentType) {
        HttpResult result = send("POST", path, body, contentType);
        if (result.status() == 403 && crumb != null) {
            // crumb 可能随会话过期：清空后重新获取并重试一次
            synchronized (this) {
                crumb = null;
                crumbField = null;
                crumbResolved = false;
            }
            result = send("POST", path, body, contentType);
        }
        return result;
    }

    private HttpResult send(String method, String path, String body, String contentType) {
        String url = baseUrl + path;
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new JenkinsDispatchException("Jenkins 地址不合法：" + path, false, e);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Accept", "application/json");
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (!"GET".equals(method)) {
            ensureCrumb();
            if (crumb != null) {
                builder.header(crumbField, crumb);
            }
        }
        if (body != null) {
            builder.header("Content-Type", contentType);
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info("Jenkins {} {} -> {}", method, path, response.statusCode());
            return new HttpResult(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException e) {
            // 连接超时/读写中断时请求可能已送达 Jenkins，运行应进入结果待确认
            log.error("Jenkins {} {} failed", method, path, e);
            throw new JenkinsDispatchException("Jenkins 请求失败：" + path, true, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsDispatchException("Jenkins 请求被中断：" + path, true, e);
        }
    }

    private void ensureCrumb() {
        if (crumbResolved) {
            return;
        }
        synchronized (this) {
            if (crumbResolved) {
                return;
            }
            HttpResult result = get("/crumbIssuer/api/json");
            if (result.status() == 200) {
                try {
                    JsonNode root = JsonUtil.readTree(result.body());
                    if (root != null) {
                        String field = root.path("crumbRequestField").asText("");
                        String value = root.path("crumb").asText("");
                        if (!field.isBlank() && !value.isBlank()) {
                            crumbField = field;
                            crumb = value;
                        }
                    }
                } catch (UtilException e) {
                    // crumb 响应异常时按无 crumb 继续，POST 失败会给出明确状态码
                    log.warn("Jenkins crumb response parse failed", e);
                }
            } else if (result.status() != 404) {
                // CSRF 关闭时接口可能不存在；其他状态保留告警，后续 POST 失败会给出明确提示
                log.warn("Jenkins crumb unavailable: HTTP {}", result.status());
            }
            crumbResolved = true;
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Jenkins 响应。
     *
     * @param status  HTTP 状态码
     * @param body    响应体
     * @param headers 响应头
     */
    record HttpResult(int status, String body, Map<String, List<String>> headers) {

        boolean isOk() {
            return status >= 200 && status < 300;
        }

        String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .findFirst()
                    .map(entry -> entry.getValue().isEmpty() ? null : entry.getValue().get(0))
                    .orElse(null);
        }
    }
}
