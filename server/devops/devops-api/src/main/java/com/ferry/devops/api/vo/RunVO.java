package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 运行概要
 */
@Data
public class RunVO {

    @Schema(description = "运行id")
    private Long id;

    @Schema(description = "运行编号")
    private String runNo;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "流水线名称")
    private String pipelineName;

    @Schema(description = "流水线版本id")
    private Long pipelineVersionId;

    @Schema(description = "流水线版本号")
    private Integer pipelineVersionNo;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "提交")
    private String commitId;

    @Schema(description = "1 Commit 2 Tag/Ref")
    private Integer refType;

    @Schema(description = "原始引用值")
    private String refValue;

    @Schema(description = "触发人")
    private Long triggerUser;

    @Schema(description = "触发说明")
    private String triggerNote;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "阻塞原因")
    private Integer blockReason;

    @Schema(description = "预校验是否通过")
    private Integer isPrecheckPass;

    @Schema(description = "幂等键")
    private String idempotentKey;

    @Schema(description = "重跑自")
    private Long rerunFromRunId;

    @Schema(description = "复用自")
    private Long reusedRunId;

    @Schema(description = "产出制品")
    private Long artifactId;

    @Schema(description = "Jenkins 构建号")
    private Long jenkinsBuildNo;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "耗时毫秒")
    private Long duration;

    @Schema(description = "创建时间")
    private Date createTime;
}
