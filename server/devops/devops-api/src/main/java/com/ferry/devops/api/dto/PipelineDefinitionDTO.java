package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流水线结构化定义（content_json 对应结构）
 */
@Data
public class PipelineDefinitionDTO {

    @Schema(description = "定义 schema 版本")
    private Integer schemaVersion = 1;

    @Schema(description = "阶段列表，1–8 个")
    private List<PipelineStageDTO> stages;
}
