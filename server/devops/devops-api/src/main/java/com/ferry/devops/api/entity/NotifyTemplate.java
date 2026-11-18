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
 * 通知模板
 */
@Data
@TableName("sys_notify_template")
@EqualsAndHashCode(callSuper = true)
public class NotifyTemplate extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 事件 */
    private String event;

    /** 渠道类型 */
    private Integer channelType;

    /** 标题模板 */
    private String titleTemplate;

    /** 正文模板 */
    private String bodyTemplate;

    /** 变量清单 */
    private String variables;

    /** 启停 */
    private Integer isEnabled;
}
