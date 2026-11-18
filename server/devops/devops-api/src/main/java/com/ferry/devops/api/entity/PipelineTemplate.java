package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线模板
 */
@Data
@TableName(value = "dev_pipeline_template")
@EqualsAndHashCode(callSuper = true)
public class PipelineTemplate extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 1 组织级 2 团队级 */
    private Integer scope;

    /** 团队id，团队级必填 */
    private Long teamId;

    /** 模板名称 */
    private String name;

    /** 适用场景 */
    private String scene;

    /** 启停 */
    private Integer isEnabled;

    /** 当前版本 */
    private Long currentVersionId;

    /** 派生数，默认 0 */
    private Integer derivedCount;
}
