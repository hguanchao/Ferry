package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 定义阶段 VO
 */
@Data
public class PipelineStageVO {

    @Schema(description = "阶段类型 1–8")
    private Integer type;

    @Schema(description = "阶段名")
    private String name;

    @Schema(description = "超时秒数")
    private Integer timeoutSeconds;

    @Schema(description = "任务列表")
    private List<PipelineTaskVO> tasks;

    @Schema(description = "产出物")
    private List<String> outputs;
}
