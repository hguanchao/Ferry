package com.ferry.devops.biz.integration.ai;

import org.springframework.stereotype.Component;

/**
 * 受限提示词拼装器：将业务类型与证据列表拼为固定结构的提示词，
 * 要求模型输出 category/conclusion/confidence/evidence/suggestion 的 JSON。
 */
@Component
public class PromptBuilder {

    /**
     * 拼装 AI 归因提示词。
     *
     * @param type      输出类型描述
     * @param bizId     业务id
     * @param evidences 证据定位文本(已校验)
     * @return 完整提示词
     */
    public String build(String type, String bizId, String evidences) {
        return "你是 DevOps 归因助手。业务类型：" + type + "，业务id：" + bizId + "。\n"
                + "请仅依据以下证据输出固定 JSON：{category,conclusion,confidence,evidence,suggestion}。\n"
                + "不得臆造证据；无可靠证据时 conclusion 输出“无有效证据，无法归因”。\n"
                + "证据列表：\n" + evidences;
    }
}
