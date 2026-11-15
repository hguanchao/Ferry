package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流水线模板基本信息
 */
@Data
public class PipelineTemplateDTO {

    @Schema(description = "模板id")
    private Long id;

    @Schema(description = "模板层级 0：组织级 1：团队级")
    @NotNull(message = "请选择模板层级")
    private Integer scope;

    @Schema(description = "归属部门id，团队级必填")
    private Long deptId;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过 100 个字符")
    private String name;

    @Schema(description = "适用场景")
    @Size(max = 500, message = "适用场景不能超过 500 个字符")
    private String description;

    @Schema(description = "初始定义，仅新增时使用")
    @Valid
    private PipelineTemplateDefinitionDTO definition;

    @Schema(description = "初始版本变更说明，仅新增时使用，为空时默认「初始版本」")
    @Size(max = 500, message = "变更说明不能超过 500 个字符")
    private String changeNote;
}
