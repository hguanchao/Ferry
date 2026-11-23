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

    @Schema(name = "任务名称")
    @NotBlank(message = "任务名不能为空")
    private String name;

    @Schema(name = "执行器", description = "1jenkins 2deployer 3gate 4release")
    private Integer executor;

    @Schema(name = "脚本引用")
    @Size(max = 200, message = "脚本引用长度不能超过 200")
    private String scriptRef;

    @Schema(name = "自定义脚本内容")
    private String scriptContent;

    @Schema(name = "脚本参数")
    private String scriptParams;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "阶段id")
    private Long stageId;

    @Schema(name = "排序号")
    private Integer sort;
}
