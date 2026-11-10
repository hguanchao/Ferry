package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfigDTO {

    @Schema(description = "参数id")
    private Long id;

    @Schema(description = "参数名称")
    @NotBlank(message = "参数名称不能为空")
    private String name;

    @Schema(description = "参数键名")
    @NotBlank(message = "参数键名不能为空")
    private String code;

    @Schema(description = "参数键值")
    @NotBlank(message = "参数值不能为空")
    private String value;

    @Schema(description = "是否系统内置 0：否 1：是")
    private Integer isInternal;
}
