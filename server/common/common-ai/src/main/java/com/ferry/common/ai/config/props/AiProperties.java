package com.ferry.common.ai.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 客户端配置。
 *
 * <p>模型、鉴权与采样参数沿用 {@code spring.ai.openai.*}，不在此重复定义；
 * 用户提示词由调用方自行构造，本配置只收敛调用开关与系统提示词。
 */
@Data
@ConfigurationProperties(prefix = "ferry.ai")
public class AiProperties {

    /**
     * 是否启用模型调用；关闭时所有请求直接降级
     */
    private boolean enabled = true;

    /**
     * 全局系统提示词；由使用方项目按自身业务配置，本脚手架不预设任何业务语义。
     *
     * <p>为空时不注入 system 消息。可由各项目的 Nacos 配置动态覆盖。
     */
    private String systemPrompt;

    /**
     * entity 调用时是否校验响应 JSON 并在失败后自动重试（默认 3 次）。
     *
     * <p>对偶发返回非 JSON 文本的模型有价值；流式调用不支持。
     */
    private boolean validateSchema = true;

    /**
     * entity 调用时是否使用模型提供方的原生结构化输出（API 级 Schema 约束）。
     *
     * <p>默认关闭：各模型支持程度不一，且 OpenAI Structured Outputs 不接受顶层 JSON 数组，
     * 请求 {@code List<T>} 会失败。确认底层模型支持后再开启。
     */
    private boolean structuredOutput = false;
}
