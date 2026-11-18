package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 证据
 */
@Data
@TableName("dev_ai_evidence")
@EqualsAndHashCode(callSuper = true)
public class AiEvidence extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 输出id */
    private Long outputId;

    /** 1 日志 2 指标 3 变更 4 历史 */
    private Integer type;

    /** 定位 */
    private String locator;

    /** 原文片段 */
    private String snippet;

    /** 服务端校验是否通过 */
    private Integer isValid;

    /** 顺序 */
    private Integer sort;
}
