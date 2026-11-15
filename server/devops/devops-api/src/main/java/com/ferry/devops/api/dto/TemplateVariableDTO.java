package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模板占位变量
 */
@Data
public class TemplateVariableDTO {

    @Schema(description = "变量名，字母开头，仅字母数字下划线，定义中以 {{变量名}} 引用")
    @NotBlank(message = "变量名不能为空")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,49}", message = "变量名必须是字母开头、仅含字母数字下划线且不超过 50 位")
    private String key;

    @Schema(description = "变量显示名")
    @NotBlank(message = "变量显示名不能为空")
    @Size(max = 50, message = "变量显示名不能超过 50 个字符")
    private String label;

    @Schema(description = "变量说明")
    @Size(max = 200, message = "变量说明不能超过 200 个字符")
    private String description;

    @Schema(description = "默认值，派生时未填写的变量按默认值替换")
    @Size(max = 500, message = "变量默认值不能超过 500 个字符")
    private String defaultValue;

    @Schema(description = "派生时是否必填")
    @NotNull(message = "是否必填不能为空")
    private Boolean required;
}
