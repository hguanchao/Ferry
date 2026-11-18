package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部署前校验表单
 */
@Data

public class DeployPrecheckDTO {

    @Schema(description = "应用id")
    @NotNull(message = "参数不能为空")
    private Long appId;

    @Schema(description = "环境id")
    @NotNull(message = "参数不能为空")
    private Long envId;

    @Schema(description = "制品id")
    @NotNull(message = "参数不能为空")
    private Long artifactId;

    @Schema(description = "变更单id")
    private String changeTicketId;
}
