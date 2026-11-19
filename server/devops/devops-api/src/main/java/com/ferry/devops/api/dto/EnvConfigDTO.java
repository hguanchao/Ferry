package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 环境配置表单
 */
@Data
public class EnvConfigDTO {

    @Schema(description = "配置id")
    private Long id;

    @Schema(description = "环境id")
    @NotNull(message = "环境id不能为空")
    private Long envId;

    @Schema(description = "配置键")
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型 1普通 2敏感")
    @NotNull(message = "值类型不能为空")
    private Integer valueType;

    @Schema(description = "是否加密")
    private Integer isEncrypted;
}