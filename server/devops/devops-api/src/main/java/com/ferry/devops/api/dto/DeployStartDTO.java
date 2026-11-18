package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部署启动表单
 */
@Data

public class DeployStartDTO {

    @Schema(description = "校验令牌")
    @NotBlank(message = "参数不能为空")
    private String precheckToken;

    @Schema(description = "应用id")
    @NotNull(message = "参数不能为空")
    private Long appId;

    @Schema(description = "环境id")
    @NotNull(message = "参数不能为空")
    private Long envId;

    @Schema(description = "制品id")
    @NotNull(message = "参数不能为空")
    private Long artifactId;

    @Schema(description = "批次计划JSON")
    private String batchPlan;

    @Schema(description = "确认人id（生产必填且不同于操作人）")
    private Long confirmer;

    @Schema(description = "确认人id（前端别名，与 confirmer 二选一）")
    private Long confirmerId;

    /** 取确认人id */
    public Long resolveConfirmer() {
        return confirmer != null ? confirmer : confirmerId;
    }
}
