package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 每日指标
 */
@Data
@TableName("dev_metric_daily")
@EqualsAndHashCode(callSuper = true)
public class MetricDaily extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 统计日 */
    private Date statDate;

    /** 应用id */
    private Long appId;

    /** 部署频率 */
    private Integer deployFreq;

    /** 变更前置时间秒 */
    private Long leadTimeP50;

    /** 变更失败率 */
    private BigDecimal changeFailRate;

    /** 恢复时长秒 */
    private Long mttrP50;

    /** 豁免率 */
    private BigDecimal gateExemptRate;

    /** 数据获取失败率 */
    private BigDecimal gateFetchFailRate;

    /** AI 采纳率 */
    private BigDecimal aiAdoptRate;

    /** AI 低置信占比 */
    private BigDecimal aiLowConfidenceRate;

    /** AI 拒答率 */
    private BigDecimal aiRefusalRate;

    /** 动作成功率 */
    private BigDecimal actionSuccessRate;

    /** 动作撤销率 */
    private BigDecimal actionUndoRate;

    /** 1 正常 2 数据不足 */
    private Integer dataStatus;
}
