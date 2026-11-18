package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线
 */
@Data
@TableName(value = "dev_pipeline")
@EqualsAndHashCode(callSuper = true)
public class Pipeline extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 流水线名称 */
    private String name;

    /** 启用 1 是 0 否 */
    private Integer isEnabled;

    /** 停用原因 */
    private String disabledReason;

    /** 当前版本id */
    private Long currentVersionId;

    /** 负责人 */
    private Long ownerId;

    /** 最近运行id */
    private Long lastRunId;

    /** 最近运行状态冗余 */
    private Integer lastRunStatus;

    /** 最近运行时间 */
    private java.util.Date lastRunTime;
}
