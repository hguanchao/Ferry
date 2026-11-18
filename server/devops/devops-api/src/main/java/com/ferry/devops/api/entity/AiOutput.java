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
 * AI 输出
 */
@Data
@TableName("dev_ai_output")
@EqualsAndHashCode(callSuper = true)
public class AiOutput extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 1 失败归因 2 准入辅助 3 预案 4 异常归因 */
    private Integer type;

    /** 业务id */
    private String bizId;

    /** 应用id */
    private Long appId;

    /** 运行id */
    private Long runId;

    /** 发布单id */
    private Long rcId;

    /** 告警id */
    private String alertId;

    /** 根因类别 */
    private String category;

    /** 结论 */
    private String conclusion;

    /** 1 高 2 中 3 低 */
    private Integer confidence;

    /** 建议JSON */
    private String suggestionJson;

    /** 模型版本 */
    private String modelVersion;

    /** 提示版本 */
    private String promptVersion;

    /** 规则版本 */
    private String ruleVersion;

    /** 知识版本 */
    private String knowledgeVersion;

    /** 1 成功 2 丢弃 3 超时 4 无法归因 */
    private Integer status;

    /** 耗时(ms) */
    private Long duration;

    /** 丢弃原因 */
    private String discardedReason;
}
