package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模板占位变量 VO
 */
@Data
public class TemplateVariableVO {

    @Schema(description = "变量名")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "是否必填")
    private Integer isRequired;
}
