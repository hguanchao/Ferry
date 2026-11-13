package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PipelineTaskVO {

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "执行命令")
    private String command;

    @Schema(description = "超时时间（分钟）")
    private Integer timeoutMinutes;
}
