package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 阶段耗时统计
 */
@Data
@TableName("dev_stage_stat")
@EqualsAndHashCode(callSuper = true)
public class StageStat extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 统计日 */
    private Date statDate;

    /** 应用id */
    private Long appId;

    /** 阶段类型 */
    private Integer stageType;

    /** 中位耗时 */
    private Long p50;

    /** P90 耗时 */
    private Long p90;

    /** 失败数 */
    private Integer failCount;
}
