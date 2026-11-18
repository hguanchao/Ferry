package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 依赖服务表单
 */
@Data
public class AppDependencyDTO {

    @Schema(description = "数据id")
    private Long id;

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "依赖服务名")
    @NotBlank(message = "依赖服务名不能为空")
    private String depName;

    @Schema(description = "关联应用id")
    private Long depAppId;

    @Schema(description = "版本范围")
    private String versionRange;
}