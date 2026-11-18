package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 触发记录 VO
 */
@Data
public class TriggerRecordVO {

    @Schema(description = "记录id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "提交")
    private String commitId;

    @Schema(description = "触发人")
    private Long triggerUser;

    @Schema(description = "1已创建 2已合并 3已复用 4已拒绝")
    private Integer result;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "触发时间")
    private Date createTime;
}
