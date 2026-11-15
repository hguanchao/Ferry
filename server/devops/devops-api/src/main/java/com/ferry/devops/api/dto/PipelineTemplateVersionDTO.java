package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流水线模板定义保存
 */
@Data
public class PipelineTemplateVersionDTO {

    @Schema(description = "模板id")
    @NotNull(message = "模板id不能为空")
    private Long templateId;

    @Schema(description = "目标版本id，仅回退时使用")
    private Long versionId;

    @Schema(description = "变更说明，首次保存可为空")
    @Size(max = 500, message = "变更说明不能超过 500 个字符")
    private String changeNote;

    @Schema(description = "模板定义")
    @Valid
    private PipelineTemplateDefinitionDTO definition;
}
