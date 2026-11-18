package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模板版本（只插不改）
 */
@Data
@TableName(value = "dev_pipeline_template_version")
public class PipelineTemplateVersion implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 模板id */
    private Long templateId;

    /** 版本号 */
    private Integer versionNo;

    /** 定义 */
    private String contentJson;

    /** YAML */
    private String contentYaml;

    /** 摘要 */
    private String contentHash;

    /** 变更说明 */
    private String changeNote;

    /** 回退来源 */
    private Long sourceVersionId;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date createTime;
}
