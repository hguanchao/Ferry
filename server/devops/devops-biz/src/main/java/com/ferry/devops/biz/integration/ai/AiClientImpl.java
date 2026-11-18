package com.ferry.devops.biz.integration.ai;

import org.springframework.stereotype.Component;

/**
 * AI 客户端最小实现。
 *
 * <p>当前不引入 spring-ai 依赖（本地未必缓存），返回固定占位结论以保证主流程可跑通。
 * 接入真实模型时，将本类替换为基于 spring-ai {@code ChatClient} 的实现：
 * 注入 {@code ChatClient.Builder}，在 {@link #chat(String)} 中调用
 * {@code chatClient.prompt().user(prompt).call().content()} 并配置 30 秒超时降级即可。
 */
@Component
public class AiClientImpl implements AiClient {

    @Override
    public String chat(String prompt) {
        return "基于证据分析：结论待确认";
    }
}
