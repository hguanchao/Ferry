package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class PipelineVO {

    @Schema(description = "流水线id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "当前版本id")
    private Long currentVersionId;

    @Schema(description = "当前版本号")
    private Integer currentVersionNo;

    @Schema(description = "阶段数")
    private Integer stageCount;

    @Schema(description = "默认分支")
    private String defaultBranch;

    @Schema(description = "应用负责人名称，逗号分隔")
    private String ownerNames;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date updateTime;

    @Schema(description = "当前定义，仅详情返回")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PipelineDefinitionVO definition;

    @Schema(description = "来源模板id，非模板派生为空")
    private Long sourceTemplateId;

    @Schema(description = "来源模板名称")
    private String sourceTemplateName;

    @Schema(description = "派生或最后合并时的模板版本号")
    private Integer sourceTemplateVersionNo;

    @Schema(description = "派生时占位变量取值，仅详情返回")
    private Map<String, String> sourceTemplateVars;

    @Schema(description = "最后合并模板时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date sourceTemplateMergedTime;

    @Schema(description = "来源模板是否有比本记录更新的版本")
    private Boolean sourceTemplateOutdated;
}
