package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 回滚表单
 */
@Data

public class RollbackDTO {

    @Schema(description = "关联部署id")
    private Long deployId;

    @Schema(description = "环境id")
    @NotNull(message = "参数不能为空")
    private Long envId;

    @Schema(description = "回滚目标制品id")
    @NotNull(message = "参数不能为空")
    private Long artifactId;

    @Schema(description = "回滚原因")
    @NotBlank(message = "参数不能为空")
    private String reason;

    @Schema(description = "确认人id")
    private Long confirmer;
}
