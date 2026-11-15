package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 运行阶段
 *
 * <p>触发时按流水线版本快照生成，只落执行状态与时间，阶段名称与超时等元数据从快照读取；状态由执行引擎维护。
 */
@Data
@TableName(value = "dev_run_stage")
@EqualsAndHashCode(callSuper = true)
public class RunStage extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 运行id
     */
    private Long runId;

    /**
     * 阶段类型
     */
    private String stageType;

    /**
     * 阶段顺序，从 1 起
     */
    private Integer stageOrder;

    /**
     * 阶段状态 PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/BLOCKED
     */
    private String status;

    /**
     * 开始时间
     */
    private Date startedTime;

    /**
     * 结束时间
     */
    private Date finishedTime;
}
