package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运行阶段
 */
@Data
@TableName(value = "dev_run_stage")
@EqualsAndHashCode(callSuper = true)
public class RunStage extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 运行id */
    private Long runId;

    /** 阶段类型 1–8 */
    private Integer stageType;

    /** 阶段名 */
    private String stageName;

    /** 顺序 */
    private Integer seq;

    /** 状态 1–9 */
    private Integer status;

    /** 阻塞原因 */
    private Integer blockReason;

    /** 复用来源阶段 */
    private Long reusedFromStageId;

    /** Jenkins Job 标识 */
    private String jenkinsJobId;

    /** 开始时间 */
    private java.util.Date startTime;

    /** 结束时间 */
    private java.util.Date endTime;

    /** 耗时毫秒 */
    private Long duration;
}
