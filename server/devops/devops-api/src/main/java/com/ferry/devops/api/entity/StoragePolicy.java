package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 存储与保留策略
 */
@Data
@TableName(value = "sys_storage_policy")
@EqualsAndHashCode(callSuper = true)
public class StoragePolicy extends BaseEntity implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id，0 为全局 */
    private Long appId;

    /** 日志保留天数 */
    private Integer logKeepDays;

    /** 归档位置 */
    private String archivePath;
}
