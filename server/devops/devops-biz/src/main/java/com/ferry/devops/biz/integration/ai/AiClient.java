package com.ferry.devops.biz.integration.ai;

/**
 * AI 大模型客户端抽象。
 */
public interface AiClient {

    /**
     * 发送提示词并获取模型文本输出。
     *
     * @param prompt 完整提示词
     * @return 模型输出文本
     */
    String chat(String prompt);
}
