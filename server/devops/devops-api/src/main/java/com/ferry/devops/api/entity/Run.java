package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线运行
 */
@Data
@TableName(value = "dev_run")
@EqualsAndHashCode(callSuper = true)
public class Run extends BaseEntity implements Serializable {

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
     * 流水线id
     */
    private Long pipelineId;

    /**
     * 运行绑定的版本id
     */
    private Long pipelineVersionId;

    /**
     * 运行绑定的版本号
     */
    private Integer pipelineVersionNo;

    /**
     * 触发方式 1：手动（V1.2.0 起唯一运行入口，历史枚举保留扩展位）
     */
    private Integer triggerType;

    /**
     * 目标分支
     */
    private String branch;

    /**
     * 目标类型 0：提交 1：标签/引用
     */
    private Integer refType;

    /**
     * 提交id或标签/引用
     */
    private String commitId;

    /**
     * 提交人
     */
    private String commitAuthor;

    /**
     * 提交时间
     */
    private Date commitTime;

    /**
     * 触发说明
     */
    private String triggerNote;

    /**
     * 触发人名称快照
     */
    private String triggerUserName;

    /**
     * 运行状态 QUEUED/RUNNING/BLOCKED/SUCCESS/FAILED/TIMEOUT/CANCELED
     */
    private String status;

    /**
     * 复用的源运行id
     */
    private Long reusedRunId;

    /**
     * 五项预校验结论 JSON
     */
    private String precheckResult;

    /**
     * 开始时间
     */
    private Date startedTime;

    /**
     * 结束时间
     */
    private Date finishedTime;
}
