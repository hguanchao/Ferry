package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateVariableVO {

    @Schema(description = "变量名，定义中以 {{变量名}} 引用")
    private String key;

    @Schema(description = "变量显示名")
    private String label;

    @Schema(description = "变量说明")
    private String description;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "派生时是否必填")
    private Boolean required;
}
