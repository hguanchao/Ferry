package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流水线阶段任务
 */
@Data
public class PipelineTaskDTO {

    @Schema(description = "任务名称")
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称不能超过 100 个字符")
    private String name;

    @Schema(description = "执行命令")
    @Size(max = 2000, message = "执行命令不能超过 2000 个字符")
    private String command;

    @Schema(description = "超时时间（分钟）")
    @NotNull(message = "任务超时时间不能为空")
    @Min(value = 1, message = "任务超时时间不能小于 1 分钟")
    @Max(value = 1440, message = "任务超时时间不能超过 1440 分钟")
    private Integer timeoutMinutes;
}
