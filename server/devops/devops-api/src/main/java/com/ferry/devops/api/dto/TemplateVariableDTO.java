package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模板占位变量
 */
@Data
public class TemplateVariableDTO {

    @Schema(description = "变量名")
    @NotBlank(message = "变量名不能为空")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "是否必填")
    private Integer isRequired;
}
