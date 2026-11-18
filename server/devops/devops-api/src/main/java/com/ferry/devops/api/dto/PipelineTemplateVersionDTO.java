package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模板版本操作：保存新版本 / 回退 / 逐条合并
 */
@Data
public class PipelineTemplateVersionDTO {

    @Schema(description = "模板id")
    private Long templateId;

    @Schema(description = "流水线id（合并时）")
    private Long pipelineId;

    @Schema(description = "模板版本id")
    private Long versionId;

    @Schema(description = "模板版本id（合并时别名）")
    private Long templateVersionId;

    @Schema(description = "定义 JSON 文本")
    private String definition;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "回退来源版本id")
    private Long sourceVersionId;

    @Schema(description = "逐条合并项，每项含 path/op/value")
    private List<Map<String, Object>> items;
}
