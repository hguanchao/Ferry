package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 流水线定义任务
 */
@Data
public class PipelineTaskDTO {

    @Schema(description = "任务名")
    @NotBlank(message = "任务名不能为空")
    private String name;

    @Schema(description = "执行器：jenkins/deployer/gate/release")
    private String executor;

    @Schema(description = "脚本引用")
    @Size(max = 200, message = "脚本引用长度不能超过 200")
    private String scriptRef;

    @Schema(description = "自定义脚本内容")
    private String scriptContent;

    @Schema(description = "任务参数")
    private Map<String, Object> params;
}
