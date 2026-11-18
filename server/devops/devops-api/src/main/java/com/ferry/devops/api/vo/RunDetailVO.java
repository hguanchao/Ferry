package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 运行详情：概要 + 阶段 + 任务
 */
@Data
public class RunDetailVO {

    @Schema(description = "运行概要")
    private RunVO run;

    @Schema(description = "阶段列表（按时序）")
    private List<RunStageVO> stages;

    @Schema(description = "全部任务列表")
    private List<RunTaskVO> tasks;
}
