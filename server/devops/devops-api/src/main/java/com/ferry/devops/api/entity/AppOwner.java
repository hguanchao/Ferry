package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用负责人
 */
@Data
@TableName(value = "dev_app_owner")
@EqualsAndHashCode(callSuper = true)
public class AppOwner extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id */
    private Long appId;

    /** 用户id */
    private Long userId;

    /** 1 负责人 2 备份负责人 */
    private Integer type;

    /** 顺序 */
    private Integer sort;
}