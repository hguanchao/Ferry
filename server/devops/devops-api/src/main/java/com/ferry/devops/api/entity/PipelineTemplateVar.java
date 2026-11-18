package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模板占位变量
 */
@Data
@TableName(value = "dev_pipeline_template_var")
public class PipelineTemplateVar implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 模板版本id */
    private Long templateVersionId;

    /** 变量名 */
    private String name;

    /** 说明 */
    private String description;

    /** 默认值 */
    private String defaultValue;

    /** 是否必填 */
    private Integer isRequired;
}
