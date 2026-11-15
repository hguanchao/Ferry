package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 流水线触发记录
 *
 * <p>每次触发（含被拒绝）都落一条，作为触发动作的审计载体。
 */
@Data
@TableName(value = "dev_run_trigger")
@EqualsAndHashCode(callSuper = true)
public class RunTrigger extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 目标分支
     */
    private String branch;

    /**
     * 提交id或标签/引用
     */
    private String commitId;

    /**
     * 触发结果 CREATED/MERGED/REUSED/REJECTED
     */
    private String result;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 关联的运行id，被拒绝时为空
     */
    private Long runId;

    /**
     * 触发人名称快照
     */
    private String triggerUserName;
}
