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
     * @param repoUrl      Git 仓库地址
     * @param branch       分支
     * @param commitId     提交或引用
     * @param items        确定性五项校验结果
     * @param jobExists    Jenkins Job 是否已同步存在
     * @return 研判结论
     */
    public RiskVerdict assess(String appName, String pipelineName, String repoUrl, String branch, String commitId,
                              List<PrecheckItemVO> items, boolean jobExists) {
        String answer = AiUtil.chat(buildPrompt(appName, pipelineName, repoUrl, branch, commitId, items, jobExists));
        if (answer == null) {
            return new RiskVerdict(true, DEFAULT_RISK);
        }
        // 模型约定首行输出 PASS/FAIL，其后为风险说明；无法识别时按不拦截处理，避免误伤正常发布
        String normalized = answer.trim();
        boolean passed = !normalized.regionMatches(true, 0, "FAIL", 0, 4);
        if (StrUtil.isBlank(normalized)) {
            return new RiskVerdict(passed, DEFAULT_RISK);
        }
        String detail = stripVerdict(normalized);
        return new RiskVerdict(passed, truncate(StrUtil.isBlank(detail) ? DEFAULT_RISK : detail));
    }

    /**
     * 剥掉首行 PASS/FAIL 标记，只保留风险说明作为展示文案。
     */
    private String stripVerdict(String text) {
        int newline = text.indexOf('\n');
        if (newline < 0) {
            return "";
        }
        return text.substring(newline).trim();
    }

    /**
     * 拼装预检提示词：业务侧自定用户提示词，系统提示词由 common-ai 统一配置注入。
     *
     * <p>确定性五项已通过是前提；模型只评估规则判断不了的额外风险，尤其关注
     * 代码源（仓库归属、分支/提交来源与意图）和 Job 同步状态。
     */
    private String buildPrompt(String appName, String pipelineName, String repoUrl, String branch, String commitId,
                               List<PrecheckItemVO> items, boolean jobExists) {
        StringBuilder evidence = new StringBuilder();
        if (items != null) {
            for (PrecheckItemVO item : items) {
                evidence.append("- 项").append(item.getType()).append('：')
                        .append(ObjUtil.equals(item.getConclusion(), 1) ? "通过" : "不通过")
                        .append("，").append(item.getDetail()).append('\n');
            }
        }
        return "你是流水线触发前的风险研判助手。请判断本次触发是否存在需要拦截的额外风险。\n"
                + "\n"
                + "【触发信息】\n"
                + "应用：" + appName + "\n"
                + "流水线：" + pipelineName + "\n"
                + "Git 仓库地址：" + (StrUtil.isBlank(repoUrl) ? "未登记" : repoUrl) + "\n"
                + "分支：" + branch + "\n"
                + "提交/引用：" + commitId + "\n"
                + "Jenkins Job 已同步：" + (jobExists ? "是" : "否（Job 不存在，构建必然失败）") + "\n"
                + "\n"
                + "【确定性预检结果】\n" + evidence + "\n"
                + "【研判原则】\n"
                + "1. 上述确定性检查已通过的事实不要质疑或重复判断。\n"
                + "2. Jenkins Job 不存在是硬失败，必须输出 FAIL。\n"
                + "3. 结合 Git 仓库地址判断变更来源：仓库归属或地址与应用明显不符时可判 FAIL；无法确认时不要据此否决。\n"
                + "4. 只关注规则判断不了的额外风险：直接向主干/生产分支推送、提交来源可疑、变更意图不明等。\n"
                + "5. 没有明确风险证据时输出 PASS，不得凭空猜测或臆造风险。\n"
                + "\n"
                + "【输出格式】\n"
                + "首行只输出 PASS 或 FAIL，第二行用一句话说明理由（不超过 200 字），不要输出其他内容。";
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
