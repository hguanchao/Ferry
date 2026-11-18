package com.ferry.devops.api.vo;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 运行阶段 VO
 */
@Data
public class RunStageVO {

    @Schema(description = "阶段id")
    private Long id;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "阶段类型 1–8")
    private Integer stageType;

    @Schema(description = "阶段名")
    private String stageName;

    @Schema(description = "顺序")
    private Integer seq;

    @Schema(description = "状态 1–9")
    private Integer status;

    @Schema(description = "阻塞原因")
    private Integer blockReason;

    @Schema(description = "复用来源阶段")
    private Long reusedFromStageId;

    @Schema(description = "Jenkins Job 标识")
    private String jenkinsJobId;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "耗时毫秒")
    private Long duration;

    @Schema(description = "该阶段下的任务")
    private List<RunTaskVO> tasks;

    @Schema(description = "断点重跑计划：该阶段是否可复用")
    private Boolean reusable;
}
