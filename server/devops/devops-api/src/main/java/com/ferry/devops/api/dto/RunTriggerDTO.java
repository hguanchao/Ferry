package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手动触发
 */
@Data
public class RunTriggerDTO {

    @Schema(description = "应用id")
    @NotNull(message = "请选择应用")
    private Long appId;

    @Schema(description = "流水线id")
    @NotNull(message = "请选择流水线")
    private Long pipelineId;

    @Schema(description = "目标分支")
    @NotBlank(message = "请选择分支")
    @Size(max = 100, message = "分支不能超过 100 个字符")
    private String branch;

    @Schema(description = "提交id或标签/引用")
    @NotBlank(message = "请填写提交id或标签/引用")
    @Size(max = 64, message = "提交id或标签/引用不能超过 64 个字符")
    private String commitId;

    @Schema(description = "目标类型 0：提交 1：标签/引用")
    @NotNull(message = "请选择目标类型")
    private Integer refType;

    @Schema(description = "触发说明")
    @Size(max = 500, message = "触发说明不能超过 500 个字符")
    private String triggerNote;

    @Schema(description = "幂等命中成功运行时是否确认复用其制品")
    private Boolean reuse;
}
