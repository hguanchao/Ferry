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

    @Schema(name = "阶段类型", description = "1构建 2单测 3静态扫描 4制品入库 5部署非生产 6集成测试 7准入 8生产发布")
    @NotNull(message = "阶段类型不能为空")
    private Integer type;

    @Schema(name = "阶段名称")
    @NotBlank(message = "阶段名不能为空")
    private String name;

    @Schema(name = "构建工具", description = "Maven/Node/Gradle")
    @Size(max = 30, message = "构建工具长度不能超过 30")
    private String buildTool;

    @Schema(name = "超时秒数")
    private Integer timeout;

    @Schema(name = "产出物")
    private String outputs;

    @Schema(name = "流水线id")
    private Long pipelineId;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "排序号")
    private Integer sort;

    @Schema(name = "任务列表")
    private List<PipelineStageTaskDTO> tasks;
}
