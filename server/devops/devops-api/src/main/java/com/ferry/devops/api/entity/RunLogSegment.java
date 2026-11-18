package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 日志分段
 */
@Data
@TableName(value = "dev_run_log_segment")
public class RunLogSegment implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 运行id */
    private Long runId;

    /** 任务id */
    private Long taskId;

    /** 段号 */
    private Integer segmentNo;

    /** 起始行号 */
    private Integer startLine;

    /** 结束行号 */
    private Integer endLine;

    /** 打码后的日志 */
    private String content;

    /** 段是否被截断 */
    private Integer isTruncated;

    /** 入库时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date createTime;
}
