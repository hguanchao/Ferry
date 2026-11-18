package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 发布单制品
 */
@Data
@TableName("dev_release_artifact")
public class ReleaseArtifact implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 发布单id */
    private Long rcId;

    /** 应用id */
    private Long appId;

    /** 制品id */
    private Long artifactId;

    /** 门禁汇总快照 */
    private Integer gateConclusion;

    /** 是否锁定 */
    private Integer isLocked;
}
