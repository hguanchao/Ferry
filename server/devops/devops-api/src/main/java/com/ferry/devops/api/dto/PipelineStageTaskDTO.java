package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流水线定义任务
 */
@Data
public class PipelineStageTaskDTO {

    @Schema(description = "任务名")
    @NotBlank(message = "任务名不能为空")
    private String name;

    @Schema(description = "执行器：1jenkins 2deployer 3gate 4release")
    private Integer executor;

    @Schema(description = "脚本引用")
    @Size(max = 200, message = "脚本引用长度不能超过 200")
    private String scriptRef;

    @Schema(description = "自定义脚本内容")
    private String scriptContent;

    @Schema(description = "脚本参数（JSON 字符串）")
    private String scriptParams;
}
