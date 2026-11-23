package com.ferry.devops.biz.integration.ai;

import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.stereotype.Component;

/**
 * AI 客户端实现。
 *
 * <p>基于 Spring AI {@link ChatClient} 调 OpenAI 兼容网关，模型、采样参数与鉴权
 * 全部来自 {@code spring.ai.openai.*} 配置，不再手写 HTTP 与请求体。
 * 未配置 api-key 或调用失败均降级返回 {@link AiClient#FALLBACK_RESULT}，绝不抛异常阻断主流程。
 */
@Slf4j
@Component
public class AiClientImpl implements AiClient {

    /** 系统提示词：要求模型只输出结构化 JSON */
    private static final String SYSTEM_PROMPT = "你是研发交付分析助手，只输出JSON，字段category/conclusion/suggestions";

    private final ChatClient chatClient;
    private final OpenAiChatProperties chatProperties;

    /**
     * @param builder         Spring AI 自动装配的 ChatClient 构造器
     * @param chatProperties  OpenAI Chat 配置，仅用于读取 api-key 判断是否已配置
     */
    public AiClientImpl(ChatClient.Builder builder, OpenAiChatProperties chatProperties) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
        this.chatProperties = chatProperties;
    }

    @Override
    public String chat(String prompt) {
        // 未配置 api-key：直接降级，避免无凭据请求白白消耗超时预算
        if (chatProperties.getApiKey() == null || StrUtil.isBlank(chatProperties.getApiKey())) {
            return FALLBACK_RESULT;
        }
        try {
            String content = chatClient.prompt()
                    .user(prompt == null ? "" : prompt)
                    .call()
                    .content();
            if (StrUtil.isBlank(content)) {
                log.warn("AI chat 响应内容为空，降级返回固定结论");
                return FALLBACK_RESULT;
            }
            return content;
        } catch (RuntimeException e) {
            // 连接失败、鉴权失败、超时、响应解析异常等统一收敛为降级文本，不阻断主流程
            log.warn("AI chat 调用异常，降级返回固定结论", e);
            return FALLBACK_RESULT;
        }
    }
}
