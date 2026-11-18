package com.ferry.devops.biz.integration.ai;

/**
 * AI 大模型客户端抽象。
 */
public interface AiClient {

    /**
     * 模型不可用（未配置端点/调用失败）时的降级结论文本。
     *
     * <p>服务层据此识别降级输出，按业务要求将置信度降为低，但整体仍视为成功、不阻塞主流程。
     */
    String FALLBACK_RESULT = "基于证据分析：结论待确认";

    /**
     * 发送提示词并获取模型文本输出。
     *
     * @param prompt 完整提示词
     * @return 模型输出文本（不可用时返回 {@link #FALLBACK_RESULT}）
     */
    String chat(String prompt);
}
