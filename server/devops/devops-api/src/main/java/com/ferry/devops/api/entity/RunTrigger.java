package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 触发记录
 */
@Data
@TableName(value = "dev_run_trigger")
public class RunTrigger implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 流水线id */
    private Long pipelineId;

    /** 分支 */
    private String branch;

    /** 提交 */
    private String commitId;

    /** 触发人 */
    private Long triggerUser;

    /** 1已创建 2已合并 3已复用 4已拒绝 */
    private Integer result;

    /** 运行id */
    private Long runId;

    /** 拒绝原因 */
    private String rejectReason;

    /** 触发时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date createTime;
}
