package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 规则模式表单
 */
@Data
public class AiRulePatternDTO {

    @Schema(description = "主键(编辑时传)")
    private Long id;

    @Schema(description = "模式名")
    @NotBlank(message = "模式名不能为空")
    private String name;

    @Schema(description = "正则")
    @NotBlank(message = "正则不能为空")
    private String pattern;

    @Schema(description = "根因类别")
    @NotBlank(message = "根因类别不能为空")
    private String category;

    @Schema(description = "启停")
    @NotNull(message = "启停不能为空")
    private Integer isEnabled;
}
