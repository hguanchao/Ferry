package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 制品清理策略。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_artifact_clean_policy")
public class ArtifactCleanPolicy implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id，全局策略写 0 */
    private Long appId;

    /** 保留最近 N 个 */
    private Integer keepLatest;

    /** 永久保留生产用过的版本 */
    private Integer isKeepProd;

    /** 软删除保留天数 */
    private Integer softDeleteDays;

    /** 执行时间 */
    private String cron;

    /** 自动执行 */
    private Integer isAutoExec;
}
