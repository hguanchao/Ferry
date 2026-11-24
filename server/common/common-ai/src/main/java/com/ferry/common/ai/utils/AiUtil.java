package com.ferry.common.ai.utils;

import com.ferry.common.ai.config.props.AiProperties;
import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.core.ParameterizedTypeReference;

/**
 * AI 大模型工具类。
 *
 * <p>基于 Spring AI {@link ChatClient} 调 OpenAI 兼容网关。系统提示词取 {@code ferry.ai.system-prompt}，
 * 用户提示词由调用方传入。未配置 api-key、`ferry.ai.enabled=false` 或调用失败均返回 null，
 * 由调用方自行决定兜底策略；本类不抛异常阻断业务主流程。
 *
 * <p>{@code chat} 统一处理文本与结构化输出：传入 {@code String.class} 返回文本，
 * 传其他类型则由 Spring AI 注入 JSON Schema 并反序列化；泛型容器用 {@link ParameterizedTypeReference}。
 *
 * <p>依赖由 {@code AiAutoConfiguration} 导入本类时通过构造器写入静态字段，业务侧以静态方式调用。
 */
@Slf4j
@SuppressWarnings("unchecked")
public class AiUtil {
    private static ChatClient chatClient;
    private static AiProperties properties;
    private static OpenAiChatProperties chatProperties;

    /**
     * 构造器注入依赖，仅在首次赋值，避免多容器场景相互覆盖。
     *
     * @param builder          Spring AI 自动装配的 ChatClient 构造器
     * @param chatProperties   OpenAI Chat 配置（前缀 spring.ai.openai.chat），用于读取模型名
     * @param properties       渡口侧 AI 配置
     */
    public AiUtil(ChatClient.Builder builder, OpenAiChatProperties chatProperties, AiProperties properties) {
        if (AiUtil.chatClient == null) {
            AiUtil.chatClient = builder.build();
        }
        if (AiUtil.chatProperties == null) {
            AiUtil.chatProperties = chatProperties;
        }
        if (AiUtil.properties == null) {
            AiUtil.properties = properties;
        }
    }

    /**
     * 发送用户提示词，使用全局系统提示词，返回文本。
     *
     * @param userPrompt 用户提示词
     * @param tools      本次请求注册的工具，可为空
     * @return 模型输出文本；失败时返回 null
     */
    public static String chat(String userPrompt, Object... tools) {
        return chat(null, userPrompt, String.class, tools);
    }

    /**
     * 指定返回类型，使用全局系统提示词。
     *
     * @param userPrompt 用户提示词
     * @param type       返回类型，如 {@code String.class} 或 {@code AttribResult.class}
     * @param tools      本次请求注册的工具，可为空
     * @param <T>        返回类型
     * @return 模型输出；失败时返回 null
     */
    public static <T> T chat(String userPrompt, Class<T> type, Object... tools) {
        return chat(null, userPrompt, type, tools);
    }

    /**
     * 覆盖系统提示词并携带工具，返回文本。
     *
     * @param systemPrompt 本次请求的系统提示词；为空时取全局配置
     * @param userPrompt   用户提示词
     * @param tools        本次请求注册的工具，可为空
     * @return 模型输出文本；失败时返回 null
     */
    public static String chat(String systemPrompt, String userPrompt, Object... tools) {
        return chat(systemPrompt, userPrompt, String.class, tools);
    }

    /**
     * 核心调用：覆盖系统提示词、指定返回类型并携带工具。
     *
     * <p>{@code type} 为 {@code String.class} 时直接返回文本，其余类型走结构化输出。
     *
     * @param systemPrompt 本次请求的系统提示词；为空时取全局配置
     * @param userPrompt   用户提示词
     * @param type         返回类型
     * @param tools        本次请求注册的工具，可为空
     * @param <T>          返回类型
     * @return 模型输出；失败时返回 null
     */
    public static <T> T chat(String systemPrompt, String userPrompt, Class<T> type, Object... tools) {
        ChatClient.ChatClientRequestSpec spec = prepare(systemPrompt, tools);
        ChatClient.CallResponseSpec call = spec.user(userPrompt == null ? "" : userPrompt).call();
        if (type == String.class) {
            String content = call.content();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            return (T) content;
        }
        return call.entity(type, AiUtil::entityParamSpec);
    }

    /**
     * 指定泛型返回类型（如 {@code List<AttribResult>}），使用全局系统提示词。
     *
     * <p>Java 泛型擦除下不能用 {@code List.class}，须用 {@code new ParameterizedTypeReference<List<T>>() {}}。
     *
     * @param userPrompt 用户提示词
     * @param type       目标泛型类型引用
     * @param tools      本次请求注册的工具，可为空
     * @param <T>        返回类型
     * @return 模型输出；失败时返回 null
     */
    public static <T> T chat(String userPrompt, ParameterizedTypeReference<T> type, Object... tools) {
        return chat(null, userPrompt, type, tools);
    }

    /**
     * 指定泛型返回类型，可覆盖系统提示词并携带工具。
     *
     * @param systemPrompt 本次请求的系统提示词；为空时取全局配置
     * @param userPrompt   用户提示词
     * @param type         目标泛型类型引用
     * @param tools        本次请求注册的工具，可为空
     * @param <T>          返回类型
     * @return 模型输出；失败时返回 null
     */
    public static <T> T chat(String systemPrompt, String userPrompt, ParameterizedTypeReference<T> type, Object... tools) {
        return prepare(systemPrompt, tools)
                .user(userPrompt == null ? "" : userPrompt)
                .call().entity(type, AiUtil::entityParamSpec);
    }

    /** 当前模型标识，业务层落库记录使用 */
    public static String model() {
        String model = chatProperties == null ? null : chatProperties.getModel();
        return StrUtil.isBlank(model) ? "unknown" : model;
    }

    /**
     * 按配置设置结构化输出行为：校验重试默认开，原生结构化输出默认关。
     *
     * <p>两个选项都可独立关闭；原生输出不支持顶层 JSON 数组，请求 List 时需关闭。
     */
    private static void entityParamSpec(ChatClient.EntityParamSpec spec) {
        if (properties.isValidateSchema()) {
            spec.validateSchema();
        }
        if (properties.isStructuredOutput()) {
            spec.useProviderStructuredOutput();
        }
    }

    private static ChatClient.ChatClientRequestSpec prepare(String systemPrompt, Object[] tools) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
        if (StrUtil.isBlank(systemPrompt)) {
            systemPrompt = properties.getSystemPrompt();
        }
        if (StrUtil.isNotBlank(systemPrompt)) {
            spec.system(systemPrompt);
        }
        if (tools != null && tools.length > 0) {
            spec.tools(tools);
        }
        return spec;
    }
}
