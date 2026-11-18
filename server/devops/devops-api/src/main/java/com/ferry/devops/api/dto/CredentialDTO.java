package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 凭据表单
 */
@Data
public class CredentialDTO {

    @Schema(description = "凭据id")
    private Long id;

    @Schema(description = "凭据名称")
    @NotBlank(message = "凭据名称不能为空")
    private String name;

    @Schema(description = "凭据类型")
    @NotNull(message = "凭据类型不能为空")
    private Integer type;

    @Schema(description = "凭据明文，保存时加密存储；编辑时留空表示保持原值")
    private String value;
}