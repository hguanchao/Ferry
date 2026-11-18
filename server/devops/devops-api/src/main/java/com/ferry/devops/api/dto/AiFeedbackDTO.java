package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 反馈表单
 */
@Data
public class AiFeedbackDTO {

    @Schema(description = "输出id")
    @NotNull(message = "输出id不能为空")
    private Long outputId;

    @Schema(description = "是否有帮助 1 是 0 否")
    @NotNull(message = "是否有帮助不能为空")
    private Integer isHelpful;

    @Schema(description = "正确答案(选填)")
    private String correctAnswer;
}
