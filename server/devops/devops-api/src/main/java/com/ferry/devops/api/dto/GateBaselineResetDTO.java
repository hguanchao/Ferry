package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 存量基线重设申请表单。
 */
@Data
public class GateBaselineResetDTO {

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "重设理由")
    @NotBlank(message = "重设理由不能为空")
    private String reason;

    @Schema(description = "审批人")
    @NotNull(message = "审批人不能为空")
    private Long approverId;
}
