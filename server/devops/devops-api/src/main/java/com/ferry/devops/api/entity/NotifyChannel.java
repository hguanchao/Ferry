package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知渠道
 */
@Data
@TableName("sys_notify_channel")
@EqualsAndHashCode(callSuper = true)
public class NotifyChannel extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 渠道名 */
    private String name;

    /** 1 IM 2 邮件 3 Webhook */
    private Integer type;

    /** 连接配置(敏感项加密) */
    private String configJson;

    /** 1 正常 2 异常 */
    private Integer status;

    /** 最近发送 */
    private Date lastSendTime;

    /** 最近失败 */
    private String lastError;
}
