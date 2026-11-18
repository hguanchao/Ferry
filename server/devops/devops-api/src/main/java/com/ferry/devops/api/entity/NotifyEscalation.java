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
 * 通知升级规则
 */
@Data
@TableName("sys_notify_escalation")
@EqualsAndHashCode(callSuper = true)
public class NotifyEscalation extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 事件 */
    private String event;

    /** 未读分钟数 */
    private Integer unreadMinutes;

    /** 升级角色 */
    private Long escalateRoleId;
}
