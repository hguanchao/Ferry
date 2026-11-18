package com.ferry.devops.api.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 质量看板 VO
 */
@Data
public class AiQualityVO {

    @Schema(description = "输出总数")
    private Long totalOutput;

    @Schema(description = "采纳数")
    private Long adoptedCount;

    @Schema(description = "采纳率")
    private BigDecimal adoptRate;

    @Schema(description = "低置信结论数")
    private Long lowConfidenceCount;

    @Schema(description = "低置信占比")
    private BigDecimal lowConfidenceRate;

    @Schema(description = "拒答数")
    private Long refusalCount;

    @Schema(description = "拒答率")
    private BigDecimal refusalRate;

    @Schema(description = "动作成功数")
    private Long actionSuccessCount;

    @Schema(description = "动作撤销数")
    private Long actionUndoCount;

    @Schema(description = "动作撤销率")
    private BigDecimal actionUndoRate;
}
