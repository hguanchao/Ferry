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
 * AI 反馈
 */
@Data
@TableName("dev_ai_feedback")
@EqualsAndHashCode(callSuper = true)
public class AiFeedback extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 输出id */
    private Long outputId;

    /** 是否有帮助 */
    private Integer isHelpful;

    /** 正确答案 */
    private String correctAnswer;

    /** 反馈人 */
    private Long userId;
}
