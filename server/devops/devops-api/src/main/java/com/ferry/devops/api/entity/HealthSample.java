package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 健康检查采样
 */
@Data
@TableName("dev_health_sample")
public class HealthSample implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 部署id */
    private Long deployId;

    /** 批次id */
    private Long batchId;

    /** 检查项 */
    private Integer checkType;

    /** 采样值 */
    private String value;

    /** 阈值快照 */
    private String threshold;

    /** 结论 1通过 2不通过 3不支持 */
    private Integer conclusion;

    /** 采样时间 */
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date sampledAt;
}
