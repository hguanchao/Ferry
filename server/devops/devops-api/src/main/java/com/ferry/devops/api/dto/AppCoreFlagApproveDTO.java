package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 核心链路变更审批表单
 */
@Data
public class AppCoreFlagApproveDTO {

    @Schema(description = "申请id")
    @NotNull(message = "申请id不能为空")
    private Long applyId;

    @Schema(description = "是否通过")
    @NotNull(message = "审批结果不能为空")
    private Boolean pass;

    @Schema(description = "审批意见")
    @NotBlank(message = "审批意见不能为空")
    private String opinion;
}