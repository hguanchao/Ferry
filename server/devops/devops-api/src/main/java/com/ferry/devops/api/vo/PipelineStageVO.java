package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class PipelineStageVO {

    @Schema(description = "阶段类型")
    private String type;

    @Schema(description = "阶段名称")
    private String name;

    @Schema(description = "超时时间（分钟）")
    private Integer timeoutMinutes;

    @Schema(description = "失败后是否继续")
    private Boolean continueOnFailure;

    @Schema(description = "产出物声明")
    private List<String> artifacts;

    @Schema(description = "任务列表")
    private List<PipelineTaskVO> tasks;
}
