package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流水线详情
 */
@Data
public class PipelineDetailVO {

    @Schema(description = "流水线概要")
    private PipelineVO pipeline;

    @Schema(description = "当前版本定义")
    private PipelineDefinitionVO definition;

    @Schema(description = "版本列表")
    private List<VersionVO> versions;

    @Schema(description = "派生来源")
    private DerivationVO derivation;
}
