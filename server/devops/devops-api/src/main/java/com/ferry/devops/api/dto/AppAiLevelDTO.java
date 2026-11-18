package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用 AI 授权表单
 */
@Data
public class AppAiLevelDTO {

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "授权级别 0-3")
    @NotNull(message = "授权级别不能为空")
    private Integer level;
}
