package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 部署类型变更申请表单
 */
@Data
public class EnvTypeChangeApplyDTO {

    @Schema(description = "环境id")
    @NotNull(message = "环境id不能为空")
    private Long id;

    @Schema(description = "申请变更的目标部署类型")
    @NotNull(message = "目标部署类型不能为空")
    private Integer deployType;

    @Schema(description = "申请理由")
    @NotBlank(message = "申请理由不能为空")
    private String reason;
}