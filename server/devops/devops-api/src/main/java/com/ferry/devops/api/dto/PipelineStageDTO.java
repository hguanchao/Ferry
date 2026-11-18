package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 流水线定义阶段
 */
@Data
public class PipelineStageDTO {

    @Schema(description = "阶段类型 1–8，同一类型最多一个")
    @NotNull(message = "阶段类型不能为空")
    private Integer type;

    @Schema(description = "阶段名")
    @NotBlank(message = "阶段名不能为空")
    private String name;

    @Schema(description = "超时秒数 60–86400")
    private Integer timeoutSeconds;

    @Schema(description = "任务列表，1–30 个")
    private List<PipelineTaskDTO> tasks;

    @Schema(description = "产出物声明")
    private List<String> outputs;
}
