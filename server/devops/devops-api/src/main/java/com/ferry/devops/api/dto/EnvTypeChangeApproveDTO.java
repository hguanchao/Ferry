package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 部署类型变更审批表单
 */
@Data
public class EnvTypeChangeApproveDTO {

    @Schema(description = "环境id")
    @NotNull(message = "环境id不能为空")
    private Long id;

    @Schema(description = "目标部署类型")
    @NotNull(message = "目标部署类型不能为空")
    private Integer deployType;

    @Schema(description = "是否通过")
    @NotNull(message = "审批结果不能为空")
    private Boolean pass;
}