package com.ferry.devops.api.vo;

import java.math.BigDecimal;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 每日指标 VO
 */
@Data
public class MetricVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "统计日")
    private Date statDate;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "部署频率")
    private Integer deployFreq;

    @Schema(description = "变更前置时间秒")
    private Long leadTimeP50;

    @Schema(description = "变更失败率")
    private BigDecimal changeFailRate;

    @Schema(description = "恢复时长秒")
    private Long mttrP50;

    @Schema(description = "豁免率")
    private BigDecimal gateExemptRate;

    @Schema(description = "数据获取失败率")
    private BigDecimal gateFetchFailRate;

    @Schema(description = "AI 采纳率")
    private BigDecimal aiAdoptRate;

    @Schema(description = "AI 低置信占比")
    private BigDecimal aiLowConfidenceRate;

    @Schema(description = "AI 拒答率")
    private BigDecimal aiRefusalRate;

    @Schema(description = "动作成功率")
    private BigDecimal actionSuccessRate;

    @Schema(description = "动作撤销率")
    private BigDecimal actionUndoRate;

    @Schema(description = "1 正常 2 数据不足")
    private Integer dataStatus;
}
