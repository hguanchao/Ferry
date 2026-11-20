package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流水线脚本预览请求
 */
@Data
public class PipelinePreviewDTO {

    @Schema(description = "流水线定义 JSON")
    @NotBlank(message = "流水线定义不能为空")
    private String definition;

    @Schema(description = "1 表单 2 YAML")
    private Integer mode;
}
