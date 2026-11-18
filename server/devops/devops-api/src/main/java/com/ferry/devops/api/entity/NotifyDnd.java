package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.time.LocalTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知免打扰
 */
@Data
@TableName("sys_notify_dnd")
@EqualsAndHashCode(callSuper = true)
public class NotifyDnd extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 1 全局 2 角色 3 用户 */
    private Integer scope;

    /** 角色id */
    private Long roleId;

    /** 用户id */
    private Long userId;

    /** 开始 */
    private LocalTime startTime;

    /** 结束 */
    private LocalTime endTime;
}
