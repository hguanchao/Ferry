package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 流水线列表项
 */
@Data
public class PipelineVO {

    @Schema(description = "流水线id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "启用 1 是 0 否")
    private Integer isEnabled;

    @Schema(description = "停用原因")
    private String disabledReason;

    @Schema(description = "当前版本id")
    private Long currentVersionId;

    @Schema(description = "当前版本号")
    private Integer currentVersionNo;

    @Schema(description = "负责人")
    private Long ownerId;

    @Schema(description = "最近运行id")
    private Long lastRunId;

    @Schema(description = "最近运行状态")
    private Integer lastRunStatus;

    @Schema(description = "最近运行分支")
    private String lastBranch;

    @Schema(description = "最近运行commit")
    private String lastCommitId;

    @Schema(description = "最近运行时间")
    private Date lastRunTime;

    @Schema(description = "最近运行耗时毫秒")
    private Long lastDuration;

    @Schema(description = "创建时间")
    private Date createTime;
}
