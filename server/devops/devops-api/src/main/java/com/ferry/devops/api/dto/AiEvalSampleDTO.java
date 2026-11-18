package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 评测样本表单
 */
@Data
public class AiEvalSampleDTO {

    @Schema(description = "输出id")
    @NotNull(message = "输出id不能为空")
    private Long outputId;

    @Schema(description = "1 没帮助 2 人工修正")
    @NotNull(message = "来源不能为空")
    private Integer source;

    @Schema(description = "评分")
    private Integer score;

    @Schema(description = "备注")
    private String note;
}
