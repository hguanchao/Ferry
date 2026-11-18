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
 * 通知订阅
 */
@Data
@TableName("sys_notify_subscription")
@EqualsAndHashCode(callSuper = true)
public class NotifySubscription extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 事件 */
    private String event;

    /** 按角色订阅 */
    private Long roleId;

    /** 按用户订阅 */
    private Long userId;

    /** 渠道id */
    private Long channelId;
}
