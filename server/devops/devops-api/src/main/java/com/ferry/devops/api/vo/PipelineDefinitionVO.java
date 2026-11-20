package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流水线定义（JSON + YAML）
 */
@Data
public class PipelineDefinitionVO {

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "结构化定义 JSON")
    private String json;

    @Schema(description = "YAML 原文")
    private String yaml;

    @Schema(description = "阶段列表")
    private List<PipelineStageVO> stages;
}
