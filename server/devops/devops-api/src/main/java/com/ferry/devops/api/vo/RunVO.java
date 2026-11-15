package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class RunVO {

    @Schema(description = "运行id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "流水线名称")
    private String pipelineName;

    @Schema(description = "运行绑定的版本号")
    private Integer pipelineVersionNo;

    @Schema(description = "目标分支")
    private String branch;

    @Schema(description = "目标类型 0：提交 1：标签/引用")
    private Integer refType;

    @Schema(description = "提交id或标签/引用")
    private String commitId;

    @Schema(description = "提交人")
    private String commitAuthor;

    @Schema(description = "运行状态 QUEUED/RUNNING/BLOCKED/SUCCESS/FAILED/TIMEOUT/CANCELED")
    private String status;

    @Schema(description = "复用的源运行id")
    private Long reusedRunId;

    @Schema(description = "触发人")
    private String triggerUserName;

    @Schema(description = "触发说明")
    private String triggerNote;

    @Schema(description = "触发时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startedTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date finishedTime;
}
