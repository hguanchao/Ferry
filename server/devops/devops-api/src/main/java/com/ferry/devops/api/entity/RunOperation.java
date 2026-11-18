package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 运行干预记录
 */
@Data
@TableName(value = "dev_run_operation")
public class RunOperation implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 1暂停 2继续 3中止 */
    private Integer type;

    /** 原因 */
    private String reason;

    /** 操作人 */
    private Long operator;

    /** 时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date createTime;
}
