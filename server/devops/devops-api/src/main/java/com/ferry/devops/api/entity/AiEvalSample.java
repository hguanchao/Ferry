package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 评测样本
 */
@Data
@TableName("dev_ai_eval_sample")
@EqualsAndHashCode(callSuper = true)
public class AiEvalSample extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 输出id */
    private Long outputId;

    /** 1 没帮助 2 人工修正 */
    private Integer source;

    /** 评分 */
    private Integer score;

    /** 备注 */
    private String note;
}
