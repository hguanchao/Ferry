package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 运行任务 VO
 */
@Data
public class RunTaskVO {

    @Schema(description = "任务id")
    private Long id;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "阶段id")
    private Long stageId;

    @Schema(description = "任务名")
    private String name;

    @Schema(description = "执行器")
    private String executor;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "退出码")
    private Integer exitCode;

    @Schema(description = "耗时")
    private Long duration;

    @Schema(description = "日志行数")
    private Integer logLineCount;

    @Schema(description = "是否截断")
    private Integer isTruncated;

    @Schema(description = "创建时间")
    private Date createTime;
}
