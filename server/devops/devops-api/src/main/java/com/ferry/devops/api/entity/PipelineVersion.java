package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流水线版本快照（只插不改）
 */
@Data
@TableName(value = "dev_pipeline_version")
public class PipelineVersion implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 流水线id */
    private Long pipelineId;

    /** 自增版本号 */
    private Integer versionNo;

    /** 结构化定义 */
    private String contentJson;

    /** YAML 原文 */
    private String contentYaml;

    /** 内容 sha256 */
    private String contentHash;

    /** 变更说明 */
    private String changeNote;

    /** 回退来源版本 */
    private Long sourceVersionId;

    /** 1 表单 2 YAML 3 回退 4 模板合并 */
    private Integer sourceType;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date createTime;
}
