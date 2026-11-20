package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线阶段定义
 */
@Data
@TableName(value = "dev_pipeline_stage")
@EqualsAndHashCode(callSuper = true)
public class PipelineStage extends BaseEntity implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 阶段类型 1构建 2单测 3静态扫描 4制品入库 5部署非生产 6集成测试 7准入 8生产发布 */
    private Integer type;

    /** 阶段名称 */
    private String name;

    /** 构建工具，如 Maven/Node/Gradle */
    private String buildTool;

    /** 版本号 */
    private Integer version;

    /** 阶段总超时秒数（所有任务执行上限） */
    private Integer timeout;

    /** 产出物，多个逗号分隔 */
    private String outputs;

    /** 流水线id */
    private Long pipelineId;

    /** 排序号 */
    private Integer sort;
}
