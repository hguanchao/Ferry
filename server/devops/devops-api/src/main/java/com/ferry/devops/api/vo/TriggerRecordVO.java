package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class TriggerRecordVO {

    @Schema(description = "记录id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "目标分支")
    private String branch;

    @Schema(description = "提交id或标签/引用")
    private String commitId;

    @Schema(description = "触发结果 CREATED/MERGED/REUSED/REJECTED")
    private String result;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "关联的运行id")
    private Long runId;

    @Schema(description = "触发人")
    private String triggerUserName;

    @Schema(description = "触发时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
