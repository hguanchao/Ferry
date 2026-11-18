package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 制品环境标记。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_artifact_env")
public class ArtifactEnv implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 制品id */
    private Long artifactId;

    /** 环境id */
    private Long envId;

    /** 环境类别快照 1测试 2预发 3生产 */
    private Integer envCategory;

    /** 晋级来源环境 */
    private Long sourceEnvId;

    /** 晋级人 */
    private Long promoter;

    /** 晋级时间 */
    private Date promotedAt;
}
