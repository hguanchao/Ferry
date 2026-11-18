package com.ferry.devops.api.vo;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 模板详情
 */
@Data
public class PipelineTemplateVersionVO {

    @Schema(description = "版本id")
    private Long id;

    @Schema(description = "模板id")
    private Long templateId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "定义 JSON")
    private String contentJson;

    @Schema(description = "定义 YAML")
    private String contentYaml;

    @Schema(description = "回退来源版本")
    private Long sourceVersionId;

    @Schema(description = "占位变量")
    private List<TemplateVariableVO> variables;

    @Schema(description = "创建时间")
    private java.util.Date createTime;
}
