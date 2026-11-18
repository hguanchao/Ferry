package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 流水线模板建档/编辑表单
 */
@Data
public class PipelineTemplateDTO {

    @Schema(description = "模板id")
    private Long id;

    @Schema(description = "1 组织级 2 团队级")
    @NotNull(message = "模板作用域不能为空")
    private Integer scope;

    @Schema(description = "团队id，团队级必填")
    private Long teamId;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @Schema(description = "适用场景")
    private String scene;

    @Schema(description = "启用 1 是 0 否")
    private Integer isEnabled;

    @Schema(description = "定义 JSON 文本")
    private String definition;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "回退来源版本id")
    private Long sourceVersionId;

    @Schema(description = "占位变量")
    private List<TemplateVariableDTO> variables;
}
