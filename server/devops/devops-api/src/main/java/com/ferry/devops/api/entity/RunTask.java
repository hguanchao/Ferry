package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运行任务
 */
@Data
@TableName(value = "dev_run_task")
@EqualsAndHashCode(callSuper = true)
public class RunTask extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 阶段id */
    private Long stageId;

    /** 任务名 */
    private String name;

    /** 执行器 */
    private String executor;

    /** 状态 */
    private Integer status;

    /** 退出码 */
    private Integer exitCode;

    /** 耗时 */
    private Long duration;

    /** 日志行数，默认 0 */
    private Integer logLineCount;

    /** 是否截断 */
    private Integer isTruncated;
}
