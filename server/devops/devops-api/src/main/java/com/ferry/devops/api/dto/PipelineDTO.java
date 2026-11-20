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

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "阶段列表（新建时作为初始版本定义）")
    private List<PipelineStageDTO> stages;
}
