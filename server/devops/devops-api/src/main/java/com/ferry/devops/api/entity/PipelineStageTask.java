package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线阶段任务定义
 */
@Data
@TableName(value = "dev_pipeline_stage_task")
@EqualsAndHashCode(callSuper = true)
public class PipelineStageTask extends BaseEntity implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 任务名称 */
    private String name;

    /** 执行器 1jenkins 2deployer 3gate 4release */
    private Integer executor;

    /** 源码脚本路径 */
    private String scriptRef;

    /** 自定义脚本内容 */
    private String scriptContent;

    /** 脚本参数（JSON 字符串） */
    private String scriptParams;

    /** 版本号 */
    private Integer version;

    /** 阶段id */
    private Long stageId;

    /** 排序号 */
    private Integer sort;
}
