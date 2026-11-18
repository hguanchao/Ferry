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
 * AI 规则模式库
 */
@Data
@TableName("dev_ai_rule_pattern")
@EqualsAndHashCode(callSuper = true)
public class AiRulePattern extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 模式名 */
    private String name;

    /** 正则 */
    private String pattern;

    /** 根因类别 */
    private String category;

    /** 启停 */
    private Integer isEnabled;
}
