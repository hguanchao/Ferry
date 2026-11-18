package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 接入向导步骤表单
 */
@Data
public class WizardStepDTO {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "步骤 1 基本信息/2 代码源/3 环境")
    @NotNull(message = "步骤不能为空")
    private Integer step;

    @Schema(description = "步骤数据 JSON")
    @NotBlank(message = "步骤数据不能为空")
    private String data;

    @Schema(description = "是否跳过")
    private Boolean skipped;
}