package com.ferry.devops.biz.integration.ai;

import com.ferry.common.ai.utils.AiUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.vo.PrecheckItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 预检助手：在确定性五项校验之外，交由模型评估本次触发是否存在额外风险。
 *
 * <p>设计取舍：五项硬校验（仓库/分支/提交/定义/幂等）仍是可执行的确定性判断，
 * 模型只在其全部通过后追加一次风险评估，用于补充"是否建议放行"的判断与风险描述。
 * 模型不可用时（AiUtil.chat 返回 null）不改变放行结论。
 */
@Slf4j
@Component
public class AiPrecheckAssistant {

    /** 降级或解析失败时返回的风险描述 */
    private static final String DEFAULT_RISK = "AI 风险分析不可用，按确定性校验结果放行";

    /**
     * 对预检结果做一次风险研判。
     *
     * @param appName      应用名称
     * @param pipelineName 流水线名称
     * @param branch       分支
     * @param commitId     提交或引用
     * @param items        确定性五项校验结果
     * @return 研判结论
     */
    public RiskVerdict assess(String appName, String pipelineName, String branch, String commitId,
                              List<PrecheckItemVO> items) {
        String answer = AiUtil.chat(buildPrompt(appName, pipelineName, branch, commitId, items));
        if (answer == null) {
            return new RiskVerdict(true, DEFAULT_RISK);
        }
        // 模型约定首行输出 PASS/FAIL，其后为风险说明；无法识别时按不拦截处理，避免误伤正常发布
        String normalized = answer == null ? "" : answer.trim();
        boolean passed = !normalized.regionMatches(true, 0, "FAIL", 0, 4);
        String reason = StrUtil.isBlank(normalized) ? DEFAULT_RISK : normalized;
        return new RiskVerdict(passed, truncate(reason));
    }

    /**
     * 拼装预检提示词：业务侧自定用户提示词，系统提示词由 common-ai 统一配置注入。
     */
    private String buildPrompt(String appName, String pipelineName, String branch, String commitId,
                               List<PrecheckItemVO> items) {
        StringBuilder evidence = new StringBuilder();
        if (items != null) {
            for (PrecheckItemVO item : items) {
                evidence.append("- 项").append(item.getType()).append('：')
                        .append(ObjUtil.equals(item.getConclusion(), 1) ? "通过" : "不通过")
                        .append("，").append(item.getDetail()).append('\n');
            }
        }
        return "请评估本次流水线触发是否存在额外风险。\n"
                + "应用：" + appName + "，流水线：" + pipelineName + "，分支：" + branch
                + "，提交：" + commitId + "\n"
                + "确定性预检结果：\n" + evidence
                + "请首行只输出 PASS 或 FAIL，随后用一句话说明理由（不超过 200 字）。";
    }

    private String truncate(String text) {
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    /**
     * 风险研判结论。
     *
     * @param passed 是否建议放行
     * @param reason 结论说明
     */
    public record RiskVerdict(boolean passed, String reason) {
    }
}
