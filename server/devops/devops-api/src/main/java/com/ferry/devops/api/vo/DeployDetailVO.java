package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部署详情
 */
@Data
public class DeployDetailVO {

    @Schema(description = "部署id")
    private Long id;

    @Schema(description = "部署编号")
    private String deployNo;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "环境名称")
    private String envName;

    @Schema(description = "部署类型")
    private Integer deployType;

    @Schema(description = "制品id")
    private Long artifactId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "操作人")
    private Long operator;

    @Schema(description = "确认人")
    private Long confirmer;

    @Schema(description = "批次计划JSON")
    private String batchPlan;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date endTime;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "批次列表")
    private java.util.List<DeployBatchVO> batches;

    @Schema(description = "校验快照")
    private java.util.List<DeployPrecheckVO> prechecks;
}
