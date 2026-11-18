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
 * 通知记录
 */
@Data
@TableName("sys_notify_record")
@EqualsAndHashCode(callSuper = true)
public class NotifyRecord extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 事件 */
    private String event;

    /** 接收人 */
    private Long receiver;

    /** 渠道 */
    private Long channelId;

    /** 内容快照 */
    private String content;

    /** 1 待发 2 成功 3 失败 4 合并 */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;

    /** 失败原因 */
    private String failReason;

    /** 发送时间 */
    private Date sendTime;
}
