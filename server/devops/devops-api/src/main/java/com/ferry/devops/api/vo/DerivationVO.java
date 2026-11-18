package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 派生关系 VO
 */
@Data
public class DerivationVO {

    @Schema(description = "派生关系id")
    private Long id;

    @Schema(description = "模板id")
    private Long templateId;

    @Schema(description = "模板版本id")
    private Long templateVersionId;

    @Schema(description = "派生出的流水线id")
    private Long pipelineId;

    @Schema(description = "流水线名称")
    private String pipelineName;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "最近合并后的流水线版本")
    private Long mergedVersionId;

    @Schema(description = "是否落后于模板")
    private Integer isBehind;

    @Schema(description = "最近合并时间")
    private Date lastMergeTime;
}
