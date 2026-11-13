package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流水线定义保存
 */
@Data
public class PipelineVersionDTO {

    @Schema(description = "流水线id")
    @NotNull(message = "流水线id不能为空")
    private Long pipelineId;

    @Schema(description = "目标版本id")
    @NotNull(message = "目标版本id不能为空")
    private Long versionId;

    @Schema(description = "变更说明，首次保存可为空")
    @Size(max = 500, message = "变更说明不能超过 500 个字符")
    private String changeNote;

    @Schema(description = "流水线定义")
    @NotNull(message = "流水线定义不能为空")
    @Valid
    private PipelineDefinitionDTO definition;
}
