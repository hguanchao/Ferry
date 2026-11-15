package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 流水线模板
 */
@Data
@TableName(value = "dev_pipeline_template")
@EqualsAndHashCode(callSuper = true)
public class PipelineTemplate extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 模板层级 0：组织级 1：团队级
     */
    private Integer scope;

    /**
     * 归属部门id，组织级为空
     */
    private Long deptId;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 适用场景
     */
    private String description;

    /**
     * 状态 0：停用 1：启用
     */
    private Integer status;

    /**
     * 当前生效版本id
     */
    private Long currentVersionId;

    /**
     * 当前生效版本号
     */
    private Integer currentVersionNo;

    /**
     * 当前版本阶段数
     */
    private Integer stageCount;

    /**
     * 当前版本占位变量数
     */
    private Integer variableCount;

    /**
     * 创建人名称快照
     */
    private String createUserName;
}
