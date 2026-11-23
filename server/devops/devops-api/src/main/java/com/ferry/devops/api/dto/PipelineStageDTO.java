package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "构建工具，如 Maven/Node/Gradle")
    @Size(max = 30, message = "构建工具长度不能超过 30")
    private String buildTool;

    @Schema(description = "超时秒数 60–86400")
    private Integer timeout;

    @Schema(description = "任务列表，1–30 个")
    private List<PipelineStageTaskDTO> tasks;

    @Schema(description = "产出物声明，多个路径使用逗号分隔")
    private String outputs;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "版本号")
    private Integer version;
}
