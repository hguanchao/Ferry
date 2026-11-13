package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流水线基本信息
 *
 * <p>新增时必须同时提交初始定义；编辑只允许改名称与描述，定义变更走版本保存接口，
 * 避免同一份定义存在两个写入口。
 */
@Data
public class PipelineDTO {

    @Schema(description = "流水线id")
    private Long id;

    @Schema(description = "所属应用id")
    @NotNull(message = "请选择应用")
    private Long appId;

    @Schema(description = "流水线名称")
    @NotBlank(message = "流水线名称不能为空")
    @Size(max = 100, message = "流水线名称不能超过 100 个字符")
    private String name;

    @Schema(description = "描述")
    @Size(max = 500, message = "描述不能超过 500 个字符")
    private String description;

    @Schema(description = "初始定义，仅新增时使用")
    @Valid
    private PipelineDefinitionDTO definition;

    @Schema(description = "初始版本变更说明，仅新增时使用，为空时默认「初始版本」")
    @Size(max = 500, message = "变更说明不能超过 500 个字符")
    private String changeNote;
}
