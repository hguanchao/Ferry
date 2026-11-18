package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 基线问题。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_gate_baseline_issue")
public class GateBaselineIssue implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 基线id */
    private Long baselineId;

    /** 问题指纹 */
    private String fingerprint;

    /** 规则 */
    private String ruleId;

    /** 1 阻断 2 高危 3 中危 4 低危 */
    private Integer severity;

    /** 文件路径 */
    private String filePath;

    /** 片段哈希 */
    private String snippetHash;

    /** 1 遗留 2 已修复 3 误报 */
    private Integer status;
}
