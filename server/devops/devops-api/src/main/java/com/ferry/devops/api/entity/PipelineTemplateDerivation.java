package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模板派生关系
 */
@Data
@TableName(value = "dev_pipeline_template_derivation")
public class PipelineTemplateDerivation implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 模板id */
    private Long templateId;

    /** 派生时版本 */
    private Long templateVersionId;

    /** 派生出的流水线 */
    private Long pipelineId;

    /** 最近合并后的流水线版本 */
    private Long mergedVersionId;

    /** 是否落后于模板 */
    private Integer isBehind;

    /** 最近合并时间 */
    private java.util.Date lastMergeTime;
}
