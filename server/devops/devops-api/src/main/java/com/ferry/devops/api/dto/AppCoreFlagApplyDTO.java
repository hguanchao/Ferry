package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 核心链路变更申请表单
 */
@Data
public class AppCoreFlagApplyDTO {

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "目标值 0 关闭 1 开启")
    @NotNull(message = "目标值不能为空")
    private Integer isTargetCore;

    @Schema(description = "申请理由")
    @NotBlank(message = "申请理由不能为空")
    private String reason;
}