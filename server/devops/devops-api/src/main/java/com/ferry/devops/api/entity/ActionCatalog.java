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
 * AI 动作目录
 */
@Data
@TableName("dev_action_catalog")
@EqualsAndHashCode(callSuper = true)
public class ActionCatalog extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 动作标识 */
    private String actionId;

    /** 名称 */
    private String name;

    /** 级别 0-3 */
    private Integer level;

    /** 参数约束JSON */
    private String paramsSchema;

    /** 前置条件 */
    private String precondition;

    /** 撤销方式 */
    private String undoMode;

    /** 是否可撤销 */
    private Integer isUndoable;

    /** 频控窗口(分钟) */
    private Integer freqMinutes;

    /** 窗口内上限 */
    private Integer freqLimit;

    /** 启停 */
    private Integer isEnabled;
}
