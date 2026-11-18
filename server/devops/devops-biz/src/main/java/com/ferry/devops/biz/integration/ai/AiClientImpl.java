package com.ferry.devops.biz.integration.ai;

import com.ferry.common.core.utils.HttpUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 客户端实现。
 *
 * <p>复用 {@link HttpUtil} + {@link JsonUtil} 调 OpenAI 兼容的 {@code /chat/completions}，
 * 外部端点一律从 {@link DevopsIntegrationProperties} 读取，不新增任何 Maven 依赖。
 * 未配置端点或调用失败均降级返回 {@link AiClient#FALLBACK_RESULT}，绝不抛异常阻断主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClientImpl implements AiClient {

    /** 系统提示词：要求模型只输出结构化 JSON */
    private static final String SYSTEM_PROMPT = "你是研发交付分析助手，只输出JSON，字段category/conclusion/suggestions";

    private final DevopsIntegrationProperties properties;

    @Override
    public String chat(String prompt) {
        DevopsIntegrationProperties.Ai ai = properties.getAi();
        // 未配置端点：保持原固定文本降级，行为不变
        if (ai == null || ai.getBaseUrl() == null || ai.getBaseUrl().isBlank()) {
            return FALLBACK_RESULT;
        }

        String url = trimTrailingSlash(ai.getBaseUrl()) + "/chat/completions";
        String body = buildBody(ai.getModel(), prompt);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + (ai.getApiKey() == null ? "" : ai.getApiKey()));
        headers.put("Content-Type", "application/json");

        try {
            HttpUtil.Response resp = HttpUtil.post(url, body, headers);
            if (!resp.isOk()) {
                log.warn("AI chat 调用失败：status={}, url={}", resp.status(), url);
                return FALLBACK_RESULT;
            }
            String content = extractContent(resp.body());
            if (content == null || content.isBlank()) {
                log.warn("AI chat 响应缺少 choices[0].message.content，url={}", url);
                return FALLBACK_RESULT;
            }
            return content;
        } catch (RuntimeException e) {
            // HTTP 失败(UtilException)、JSON 解析异常等统一收敛为降级文本，不阻断主流程
            log.warn("AI chat 调用异常，降级返回固定结论，url={}", url, e);
            return FALLBACK_RESULT;
        }
    }

    /**
     * 拼装 OpenAI 兼容请求体。
     */
    private String buildBody(String model, String prompt) {
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt == null ? "" : prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(systemMsg, userMsg));
        body.put("temperature", 0);
        return JsonUtil.toJson(body);
    }

    /**
     * 从响应 JSON 中取 choices[0].message.content。
     */
    private String extractContent(String responseBody) {
        JsonNode root = JsonUtil.readTree(responseBody);
        if (root == null) {
            return null;
        }
        // at(JsonPointer) 对缺失节点返回 MissingNode，链式取值不会空指针
        JsonNode content = root.at("/choices/0/message/content");
        return content.isString() ? content.stringValue() : null;
    }

    /**
     * 去掉基址末尾的斜杠，避免拼出双斜杠。
     */
    private String trimTrailingSlash(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
