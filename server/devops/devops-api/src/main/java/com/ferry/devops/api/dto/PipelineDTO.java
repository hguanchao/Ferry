package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 流水线建档/编辑表单
 */
@Data
public class PipelineDTO {

    @Schema(description = "流水线id")
    private Long id;

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "流水线名称")
    @NotBlank(message = "流水线名称不能为空")
    private String name;

    @Schema(description = "负责人")
    private Long ownerId;

    @Schema(description = "启用 1 是 0 否")
    private Integer isEnabled;

    @Schema(description = "停用原因")
    private String disabledReason;

    @Schema(description = "新建时初始定义（可选，结构化 JSON）")
    private String definition;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "回退来源版本id")
    private Long sourceVersionId;

    @Schema(description = "阶段列表（表单模式）")
    private List<PipelineStageDTO> stages;
}
