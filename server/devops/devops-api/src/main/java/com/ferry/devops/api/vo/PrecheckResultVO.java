package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 五项预校验结果
 */
@Data
public class PrecheckResultVO {

    @Schema(description = "逐条结论")
    private List<PrecheckItemVO> items;

    @Schema(description = "是否可触发（前四项全通过且 AI 研判通过）")
    private Boolean canTrigger;

    @Schema(description = "AI 风险研判是否建议放行")
    private Boolean aiPassed;

    @Schema(description = "AI 风险研判说明")
    private String aiReason;

    @Schema(description = "幂等命中：既有运行id")
    private Long mergedRunId;

    @Schema(description = "幂等命中：可复用制品摘要")
    private String artifactDigest;
}
