package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class PipelineTemplateDefinitionVO {

    @Schema(description = "阶段列表，数组顺序即执行顺序")
    private List<PipelineStageVO> stages;

    @Schema(description = "占位变量列表")
    private List<TemplateVariableVO> variables;
}
