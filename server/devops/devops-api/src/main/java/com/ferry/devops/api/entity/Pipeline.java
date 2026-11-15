package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线
 */
@Data
@TableName(value = "dev_pipeline")
@EqualsAndHashCode(callSuper = true)
public class Pipeline extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 所属应用id
     */
    private Long appId;

    /**
     * 流水线名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

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
     * 来源模板id，非模板派生为空
     */
    private Long sourceTemplateId;

    /**
     * 派生或最后合并时的模板版本号
     */
    private Integer sourceTemplateVersionNo;

    /**
     * 派生时占位变量取值 JSON
     */
    private String sourceTemplateVars;

    /**
     * 最后合并模板时间
     */
    private Date sourceTemplateMergedTime;
}
