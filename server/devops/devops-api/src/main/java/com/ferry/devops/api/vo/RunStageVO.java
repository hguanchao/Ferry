package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RunStageVO {

    @Schema(description = "阶段记录id")
    private Long id;

    @Schema(description = "阶段类型")
    private String stageType;

    @Schema(description = "阶段名称")
    private String stageName;

    @Schema(description = "阶段顺序，从 1 起")
    private Integer stageOrder;

    @Schema(description = "阶段状态 PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/BLOCKED")
    private String status;

    @Schema(description = "超时时间（分钟）")
    private Integer timeoutMinutes;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startedTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date finishedTime;

    @Schema(description = "任务明细，来自运行绑定的版本快照")
    private List<RunTaskVO> tasks;
}
